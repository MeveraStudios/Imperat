package studio.mevera.imperat;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class AsyncTabListener<S extends BukkitCommandSource> implements Listener {

    private final BukkitImperat<S> imperat;

    public AsyncTabListener(BukkitImperat<S> imperat) {
        this.imperat = imperat;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void asyncTabComplete(AsyncTabCompleteEvent event) {
        String commandLine = event.getBuffer();
        if (commandLine.startsWith("/")) {
            commandLine = commandLine.substring(1);
        }

        S src = imperat.wrapSender(event.getSender());

        var autocompletedResults = imperat.autoComplete(src, commandLine).join();
        if (autocompletedResults.isEmpty()) {
            return;
        }

        event.setCompletions(autocompletedResults);
        event.setHandled(true);
    }

}
