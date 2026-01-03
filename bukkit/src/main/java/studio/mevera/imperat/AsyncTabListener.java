package studio.mevera.imperat;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AsyncTabListener implements Listener {

    private final BukkitImperat imperat;

    public AsyncTabListener(BukkitImperat imperat) {
        this.imperat = imperat;
    }

    @EventHandler
    public void asyncTabComplete(AsyncTabCompleteEvent event) {
        String commandLine = event.getBuffer();
        if (commandLine.startsWith("/")) {
            commandLine = commandLine.substring(1);
        }

        BukkitSource src = imperat.wrapSender(event.getSender());
        var autocomplete = imperat.autoComplete(src, commandLine).join();
        if (autocomplete.isEmpty()) {
            return;
        }

        event.setCompletions(autocomplete);
    }

}
