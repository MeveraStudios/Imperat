package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandSource;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * The Discord user who ran a command, either as a slash command or as a text message
 * ({@link JdaTextCommands}).
 *
 * <p>Commands that answer through {@link #send}, {@link #sendPrivately} and {@link #reply} work the
 * same way for both. Slash-only features (modals on the command itself) are reached through
 * {@link #slashEvent()}, which is {@code null} for a text command.</p>
 *
 * <p>Ephemeral messages only exist as an answer to an interaction, and a text message is not one.
 * {@link #sendPrivately} therefore hands text commands to the configured {@link JdaPrivateReplier}
 * (by default a "Show" button whose click answers ephemerally).</p>
 */
public final class JdaCommandSource implements CommandSource {

    private final @Nullable SlashCommandInteractionEvent slash;
    private final @Nullable MessageReceivedEvent message;
    private final @Nullable JdaPrivateReplier privateReplier;
    private final String label;
    private final String commandPath;
    /** Slash only: whether the deferred "thinking…" response has been replaced or removed. */
    private final AtomicBoolean originalUsed = new AtomicBoolean(false);

    private JdaCommandSource(
            @Nullable SlashCommandInteractionEvent slash,
            @Nullable MessageReceivedEvent message,
            @Nullable JdaPrivateReplier privateReplier,
            String label,
            String commandPath
    ) {
        this.slash = slash;
        this.message = message;
        this.privateReplier = privateReplier;
        this.label = label;
        this.commandPath = commandPath;
    }

    /** A slash command source. */
    public static @NotNull JdaCommandSource ofSlash(@NotNull SlashCommandInteractionEvent event) {
        Objects.requireNonNull(event, "event");
        return new JdaCommandSource(event, null, null, event.getName(), event.getFullCommandName());
    }

    /**
     * A text command source.
     *
     * @param event          the message that ran the command
     * @param label          the word the member typed to run it (a command name, alias or custom trigger)
     * @param commandPath    the command it ran, written like a slash command's full name ({@code "ticket staff add"})
     * @param privateReplier how {@link #sendPrivately} answers; {@code null} answers in the channel
     */
    public static @NotNull JdaCommandSource ofMessage(
            @NotNull MessageReceivedEvent event,
            @NotNull String label,
            @NotNull String commandPath,
            @Nullable JdaPrivateReplier privateReplier
    ) {
        Objects.requireNonNull(event, "event");
        return new JdaCommandSource(null, event, privateReplier, label, commandPath);
    }

    static JdaCommandSource dummy() {
        return new JdaCommandSource(null, null, null, "", "");
    }

    // ---------------------------------------------------------------- what ran the command

    /** @return whether the command ran as a slash command */
    public boolean isSlash() {
        return slash != null;
    }

    /** @return whether the command ran from a text message */
    public boolean isMessage() {
        return message != null;
    }

    /** @return the slash command event, or {@code null} for a text command */
    public @Nullable SlashCommandInteractionEvent slashEvent() {
        return slash;
    }

    /** @return the message event, or {@code null} for a slash command */
    public @Nullable MessageReceivedEvent messageEvent() {
        return message;
    }

    /** @return the message that ran a text command, or {@code null} for a slash command */
    public @Nullable Message message() {
        return message == null ? null : message.getMessage();
    }

    /** @return the event that ran the command: a {@link SlashCommandInteractionEvent} or a {@link MessageReceivedEvent} */
    @Override
    public GenericEvent origin() {
        return slash != null ? slash : message;
    }

    /** @return the word used to run the command: the slash command's name, or what the member typed */
    public @NotNull String label() {
        return label;
    }

    /**
     * @return the command that ran, written like a slash command's full name: {@code "ban"},
     * {@code "ticket staff"}, {@code "ticket staff add"}. Text commands resolve their subcommands the same way.
     */
    public @NotNull String commandPath() {
        return commandPath;
    }

    @Override
    public String name() {
        return user().getName();
    }

    @Override
    public boolean isConsole() {
        return guild() == null;
    }

    @Override
    public UUID uuid() {
        return UUID.nameUUIDFromBytes(user().getId().getBytes());
    }

    public @NotNull User user() {
        return slash != null ? slash.getUser() : requireMessage().getAuthor();
    }

    public @Nullable Member member() {
        return slash != null ? slash.getMember() : requireMessage().getMember();
    }

    public @Nullable Guild guild() {
        if (slash != null) return slash.getGuild();
        return requireMessage().isFromGuild() ? message.getGuild() : null;
    }

    public @NotNull MessageChannelUnion channel() {
        return slash != null ? slash.getChannel() : requireMessage().getChannel();
    }

    public @NotNull JDA jda() {
        return slash != null ? slash.getJDA() : requireMessage().getJDA();
    }

    // ---------------------------------------------------------------- entities the command already carries

    /**
     * A user the invocation already resolved: a slash option's user or a mentioned user.
     * Falls back to the JDA cache.
     */
    public @Nullable User findUser(long id) {
        User found = fromInvocation(id, OptionMapping::getAsUser, m -> m.getMentions().getUsers());
        return found != null ? found : jda().getUserById(id);
    }

    /** A member the invocation already resolved, falling back to the server's member cache. */
    public @Nullable Member findMember(long id) {
        Member found = fromInvocation(id, OptionMapping::getAsMember, m -> m.getMentions().getMembers());
        if (found != null) return found;
        Guild guild = guild();
        return guild == null ? null : guild.getMemberById(id);
    }

    /** A role the invocation already resolved, falling back to the server's roles. */
    public @Nullable Role findRole(long id) {
        Role found = fromInvocation(id, option -> option.getType() == OptionType.ROLE || option.getType() == OptionType.MENTIONABLE
                                                       ? option.getAsRole() : null,
                m -> m.getMentions().getRoles());
        if (found != null) return found;
        Guild guild = guild();
        return guild == null ? null : guild.getRoleById(id);
    }

    private <T extends net.dv8tion.jda.api.entities.ISnowflake> @Nullable T fromInvocation(
            long id,
            Function<OptionMapping, T> fromOption,
            Function<Message, java.util.List<T>> fromMentions
    ) {
        if (slash != null) {
            for (OptionMapping option : slash.getOptions()) {
                OptionType type = option.getType();
                if (type != OptionType.USER && type != OptionType.ROLE && type != OptionType.MENTIONABLE) continue;
                T value;
                try {
                    value = fromOption.apply(option);
                } catch (IllegalStateException wrongType) {
                    continue;
                }
                if (value != null && value.getIdLong() == id) return value;
            }
            return null;
        }
        if (message != null) {
            for (T mentioned : fromMentions.apply(message.getMessage())) {
                if (mentioned.getIdLong() == id) return mentioned;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- answering

    /**
     * Answers the command where everyone can see it. A slash command's first answer replaces its
     * "thinking…" response; a text command's answer replies to the member's message.
     */
    public @NotNull CompletableFuture<Message> send(@NotNull MessageCreateData data) {
        if (slash != null) {
            if (!slash.isAcknowledged()) {
                originalUsed.set(true);
                return slash.reply(data).submit().thenCompose(hook -> hook.retrieveOriginal().submit());
            }
            originalUsed.set(true);
            return slash.getHook().sendMessage(data).submit();
        }
        return requireMessage().getMessage().reply(data)
                       .mentionRepliedUser(false)
                       .failOnInvalidReply(false)
                       .submit();
    }

    /**
     * Answers the command so only the member sees it. A slash command answers ephemerally (removing
     * the public "thinking…" response first); a text command goes through the {@link JdaPrivateReplier}.
     */
    public @NotNull CompletableFuture<Void> sendPrivately(@NotNull MessageCreateData data) {
        if (slash != null) {
            if (!slash.isAcknowledged()) {
                originalUsed.set(true);
                return slash.reply(data).setEphemeral(true).submit().thenApply(ignored -> null);
            }
            InteractionHook hook = slash.getHook();
            CompletableFuture<Void> cleared = originalUsed.getAndSet(true)
                                                      ? CompletableFuture.completedFuture(null)
                                                      : hook.deleteOriginal().submit().exceptionally(error -> null);
            return cleared.thenCompose(ignored -> hook.sendMessage(data).setEphemeral(true).submit())
                           .thenApply(ignored -> null);
        }
        MessageReceivedEvent event = requireMessage();
        if (privateReplier == null) {
            return send(data).thenApply(ignored -> null);
        }
        return privateReplier.send(event, data);
    }

    /** Answers publicly with plain text. */
    @Override
    public void reply(String message) {
        if (slash != null) {
            respondSlash(message, false);
        } else {
            send(MessageCreateData.fromContent(message));
        }
    }

    /** A warning: ephemeral for slash commands, a reply for text commands. */
    @Override
    public void warn(String message) {
        if (slash != null) {
            respondSlash("⚠️ " + message, true);
        } else {
            send(MessageCreateData.fromContent("⚠️ " + message));
        }
    }

    /** An error: ephemeral for slash commands, a reply for text commands. */
    @Override
    public void error(String message) {
        if (slash != null) {
            respondSlash("❌ " + message, true);
        } else {
            send(MessageCreateData.fromContent("❌ " + message));
        }
    }

    private void respondSlash(String text, boolean ephemeral) {
        originalUsed.set(true);
        if (slash.isAcknowledged()) {
            slash.getHook().sendMessage(text).setEphemeral(ephemeral).queue();
        } else {
            slash.reply(text).setEphemeral(ephemeral).queue();
        }
    }

    private MessageReceivedEvent requireMessage() {
        if (message == null) {
            throw new IllegalStateException("This source has no event (a dummy source)");
        }
        return message;
    }
}
