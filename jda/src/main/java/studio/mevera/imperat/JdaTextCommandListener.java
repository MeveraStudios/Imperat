package studio.mevera.imperat;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

/** Runs text commands ({@link JdaTextCommands}) from received messages. */
final class JdaTextCommandListener implements EventListener {

    private final JdaImperat<?> imperat;

    JdaTextCommandListener(JdaImperat<?> imperat) {
        this.imperat = imperat;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof MessageReceivedEvent message) {
            imperat.dispatchText(message);
        }
    }
}
