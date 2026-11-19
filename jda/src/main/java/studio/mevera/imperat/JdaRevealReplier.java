package studio.mevera.imperat;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Answers a text command privately through a button: the bot replies with a short prompt and a
 * "Show" button, and the click (an interaction) gets the real answer as an ephemeral message.
 * Only the member who ran the command can open it. The prompt is deleted when it expires.
 *
 * <p>Answers are kept in memory until they expire; a restart forgets them (the button then says
 * the answer expired). An answer with file attachments can be shown once, since Discord consumes
 * the upload.</p>
 */
public final class JdaRevealReplier implements JdaPrivateReplier {

    static final String PREFIX = "imperat-reveal:";
    private static final Map<String, Pending> PENDING = new ConcurrentHashMap<>();

    private final Duration expiry;
    private final Function<User, String> prompt;
    private final String buttonLabel;
    private final String notYours;

    private JdaRevealReplier(Builder builder) {
        this.expiry = builder.expiry;
        this.prompt = builder.prompt;
        this.buttonLabel = builder.buttonLabel;
        this.notYours = builder.notYours;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    /** The listener answering every reveal button; {@link JdaImperat} registers it with text commands. */
    static EventListener listener() {
        return Listener.INSTANCE;
    }

    /** @return how many answers are waiting to be shown (for tests) */
    static int pendingCount() {
        return PENDING.size();
    }

    @Override
    public @NotNull CompletableFuture<Void> send(@NotNull MessageReceivedEvent event, @NotNull MessageCreateData data) {
        String key = UUID.randomUUID().toString();
        User author = event.getAuthor();
        PENDING.put(key, new Pending(author.getIdLong(), data, notYours));
        CompletableFuture.delayedExecutor(expiry.toMillis(), TimeUnit.MILLISECONDS).execute(() -> PENDING.remove(key));

        MessageCreateData promptMessage = new MessageCreateBuilder()
                                                  .setContent(prompt.apply(author))
                                                  .setComponents(ActionRow.of(Button.secondary(PREFIX + key, buttonLabel)))
                                                  .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                                                  .build();
        return event.getMessage().reply(promptMessage)
                       .mentionRepliedUser(false)
                       .failOnInvalidReply(false)
                       .submit()
                       .thenAccept(sent -> sent.delete().queueAfter(expiry.toMillis(), TimeUnit.MILLISECONDS,
                               null, ignored -> { }))
                       .whenComplete((ignored, failure) -> {
                           if (failure != null) PENDING.remove(key);
                       });
    }

    private record Pending(long userId, MessageCreateData data, String notYours) {
    }

    private enum Listener implements EventListener {
        INSTANCE;

        @Override
        public void onEvent(@NotNull GenericEvent event) {
            if (!(event instanceof ButtonInteractionEvent click)) return;
            String id = click.getComponentId();
            if (!id.startsWith(PREFIX)) return;
            Pending pending = PENDING.get(id.substring(PREFIX.length()));
            if (pending == null) {
                click.reply("This answer has expired. Run the command again.").setEphemeral(true).queue();
                return;
            }
            if (pending.userId() != click.getUser().getIdLong()) {
                click.reply(pending.notYours()).setEphemeral(true).queue();
                return;
            }
            click.reply(pending.data()).setEphemeral(true).queue();
        }
    }

    public static final class Builder {

        private Duration expiry = Duration.ofMinutes(5);
        private Function<User, String> prompt = user -> "🔒 " + user.getAsMention() + ", your answer is ready. Only you can see it.";
        private String buttonLabel = "Show";
        private String notYours = "This answer belongs to someone else.";

        private Builder() {
        }

        /** How long the button works; the prompt is deleted afterwards. Default 5 minutes. */
        public Builder expiry(@NotNull Duration expiry) {
            if (expiry.isNegative() || expiry.isZero()) throw new IllegalArgumentException("expiry must be positive");
            this.expiry = expiry;
            return this;
        }

        /** The public prompt's text, given the member who ran the command. */
        public Builder prompt(@NotNull Function<User, String> prompt) {
            this.prompt = Objects.requireNonNull(prompt);
            return this;
        }

        public Builder buttonLabel(@NotNull String label) {
            this.buttonLabel = Objects.requireNonNull(label);
            return this;
        }

        /** The ephemeral answer to anyone else who clicks the button. */
        public Builder notYours(@NotNull String text) {
            this.notYours = Objects.requireNonNull(text);
            return this;
        }

        public @NotNull JdaRevealReplier build() {
            return new JdaRevealReplier(this);
        }
    }
}
