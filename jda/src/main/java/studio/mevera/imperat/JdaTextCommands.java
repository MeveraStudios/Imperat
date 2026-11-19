package studio.mevera.imperat;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Lets the registered commands also run from text messages, such as {@code !ban @user 1d}.
 *
 * <p>A message runs a command when it starts with one of the {@link Builder#prefixes prefixes} (or a
 * mention of the bot), followed by a trigger word. The trigger is looked up with the
 * {@link Builder#triggers trigger resolver} first, then (with {@link Builder#commandNames}) among the
 * registered command names and aliases. Everything after the trigger is parsed like any text
 * command input, so subcommands, quotes, flags and greedy arguments behave as on other platforms.</p>
 *
 * <p>Text commands need the {@code MESSAGE_CONTENT} gateway intent (mentions of the bot work without it).</p>
 *
 * <pre>{@code
 * JdaImperat.builder(jda)
 *     .textCommands(JdaTextCommands.builder()
 *         .prefixes(event -> List.of("!"))
 *         .triggers((event, word) -> word.equals("b") ? "ban" : null)
 *         .build())
 *     .build();
 * }</pre>
 */
public final class JdaTextCommands {

    private final Function<MessageReceivedEvent, Collection<String>> prefixes;
    private final boolean mentionPrefix;
    private final boolean commandNames;
    private final JdaTextTriggerResolver triggers;
    private final JdaPrivateReplier privateReplier;
    private final Predicate<MessageReceivedEvent> filter;
    private final boolean ignoreBots;
    private final boolean typing;

    private JdaTextCommands(Builder builder) {
        this.prefixes = builder.prefixes;
        this.mentionPrefix = builder.mentionPrefix;
        this.commandNames = builder.commandNames;
        this.triggers = builder.triggers;
        this.privateReplier = builder.privateReplier;
        this.filter = builder.filter;
        this.ignoreBots = builder.ignoreBots;
        this.typing = builder.typing;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    Collection<String> prefixes(MessageReceivedEvent event) {
        Collection<String> found = prefixes.apply(event);
        return found == null ? List.of() : found;
    }

    boolean mentionPrefix() {
        return mentionPrefix;
    }

    boolean commandNames() {
        return commandNames;
    }

    JdaTextTriggerResolver triggers() {
        return triggers;
    }

    JdaPrivateReplier privateReplier() {
        return privateReplier;
    }

    boolean accepts(MessageReceivedEvent event) {
        if (ignoreBots && (event.getAuthor().isBot() || event.isWebhookMessage())) return false;
        return filter.test(event);
    }

    boolean typing() {
        return typing;
    }

    public static final class Builder {

        private Function<MessageReceivedEvent, Collection<String>> prefixes = event -> List.of("!");
        private boolean mentionPrefix = true;
        private boolean commandNames = true;
        private JdaTextTriggerResolver triggers = (event, trigger) -> null;
        private JdaPrivateReplier privateReplier = JdaPrivateReplier.revealButton();
        private Predicate<MessageReceivedEvent> filter = event -> true;
        private boolean ignoreBots = true;
        private boolean typing = true;

        private Builder() {
        }

        /**
         * The prefixes for this message (usually per server). An empty string makes prefix-less
         * triggers work: the first word of every message is looked up. Default {@code "!"}.
         */
        public Builder prefixes(@NotNull Function<MessageReceivedEvent, Collection<String>> prefixes) {
            this.prefixes = Objects.requireNonNull(prefixes);
            return this;
        }

        /** Fixed prefixes for every server. */
        public Builder prefixes(@NotNull String... prefixes) {
            List<String> fixed = List.of(prefixes);
            this.prefixes = event -> fixed;
            return this;
        }

        /** Whether {@code @Bot ban @user} works as well as a prefix. Default on. */
        public Builder mentionPrefix(boolean mentionPrefix) {
            this.mentionPrefix = mentionPrefix;
            return this;
        }

        /**
         * Whether a registered command's own name and aliases work as triggers. Off means only the
         * {@link #triggers trigger resolver} decides which words run commands. Default on.
         */
        public Builder commandNames(boolean commandNames) {
            this.commandNames = commandNames;
            return this;
        }

        /** Custom trigger words, such as aliases a server set; looked up before command names. */
        public Builder triggers(@NotNull JdaTextTriggerResolver triggers) {
            this.triggers = Objects.requireNonNull(triggers);
            return this;
        }

        /** How {@link JdaCommandSource#sendPrivately} answers text commands. Default {@link JdaPrivateReplier#revealButton()}. */
        public Builder privateReplies(@NotNull JdaPrivateReplier replier) {
            this.privateReplier = Objects.requireNonNull(replier);
            return this;
        }

        /** Messages that may run text commands at all (for example, only from licensed servers). */
        public Builder filter(@NotNull Predicate<MessageReceivedEvent> filter) {
            this.filter = Objects.requireNonNull(filter);
            return this;
        }

        /** Whether messages from bots and webhooks are ignored. Default on. */
        public Builder ignoreBots(boolean ignoreBots) {
            this.ignoreBots = ignoreBots;
            return this;
        }

        /** Whether the bot shows "typing…" while a text command runs, as slash commands show "thinking…". Default on. */
        public Builder typing(boolean typing) {
            this.typing = typing;
            return this;
        }

        public @NotNull JdaTextCommands build() {
            return new JdaTextCommands(this);
        }
    }
}
