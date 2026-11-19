package studio.mevera.imperat;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maps a typed trigger word to the command it runs, for {@link JdaTextCommands}.
 *
 * <p>The answer is a command path written like a slash command's full name: {@code "ban"},
 * {@code "ticket staff"} or {@code "ticket staff add"}. The words after the trigger follow the path,
 * so a trigger for {@code "ticket staff"} typed as {@code !ts add @Mods} runs {@code ticket staff add @Mods}.</p>
 */
@FunctionalInterface
public interface JdaTextTriggerResolver {

    /**
     * @param event   the message
     * @param trigger the first word after the prefix, lower-cased
     * @return the command path it runs, or {@code null} when it is not a trigger here
     */
    @Nullable String resolve(@NotNull MessageReceivedEvent event, @NotNull String trigger);
}
