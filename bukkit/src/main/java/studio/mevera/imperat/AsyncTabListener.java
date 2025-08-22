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
        BukkitSource src = imperat.wrapSender(event.getSender());
        String commandLine = event.getBuffer();
        if(commandLine.startsWith("/")) {
            commandLine = commandLine.substring(1);
        }
        event.setCompletions(
                imperat.autoComplete(src, commandLine).join()
        );
    }
}
