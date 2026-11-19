package studio.mevera.imperat;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * How a text command answers privately ({@link JdaCommandSource#sendPrivately}).
 *
 * <p>Discord only allows ephemeral messages as an answer to an interaction (a slash command, a
 * button, a menu, a modal). A text message is not an interaction, so the choices are:</p>
 * <ul>
 *     <li>{@link #revealButton()}: a short public reply with a "Show" button. Clicking it is an
 *     interaction, so the real answer arrives ephemerally, to the member who ran the command only.
 *     Buttons and menus in the answer keep working.</li>
 *     <li>{@link #directMessage(JdaPrivateReplier)}: the answer goes to the member's DMs.</li>
 *     <li>{@link #inChannel()}: the answer is posted publicly.</li>
 * </ul>
 *
 * <p>A replier that is also a JDA {@code EventListener} is registered on JDA by {@link JdaImperat}.</p>
 */
@FunctionalInterface
public interface JdaPrivateReplier {

    /**
     * Sends {@code data} so that only the author of {@code event} sees it.
     *
     * @return a future completing once the answer (or its stand-in) has been sent
     */
    @NotNull CompletableFuture<Void> send(@NotNull MessageReceivedEvent event, @NotNull MessageCreateData data);

    /** A "Show" button that answers ephemerally when the member clicks it; kept for 5 minutes. */
    static @NotNull JdaRevealReplier revealButton() {
        return JdaRevealReplier.builder().build();
    }

    /** A "Show" button kept for {@code expiry}. */
    static @NotNull JdaRevealReplier revealButton(@NotNull Duration expiry) {
        return JdaRevealReplier.builder().expiry(expiry).build();
    }

    /** Sends the answer to the member's DMs; when DMs are closed, {@code fallback} answers instead. */
    static @NotNull JdaPrivateReplier directMessage(@NotNull JdaPrivateReplier fallback) {
        return (event, data) -> event.getAuthor().openPrivateChannel()
                                        .flatMap(channel -> channel.sendMessage(data))
                                        .submit()
                                        .<Void>thenApply(ignored -> null)
                                        .exceptionallyCompose(closed -> fallback.send(event, data));
    }

    /** Answers in the channel, where everyone can see it. */
    static @NotNull JdaPrivateReplier inChannel() {
        return (event, data) -> event.getMessage().reply(data)
                                        .mentionRepliedUser(false)
                                        .failOnInvalidReply(false)
                                        .submit()
                                        .thenApply(ignored -> null);
    }
}
