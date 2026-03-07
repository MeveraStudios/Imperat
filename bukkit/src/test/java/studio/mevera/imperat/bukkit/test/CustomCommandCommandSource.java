package studio.mevera.imperat.bukkit.test;

import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.context.CommandSource;

public class CustomCommandCommandSource implements CommandSource {

    private final BukkitCommandSource platformSource;

    public CustomCommandCommandSource(BukkitCommandSource platformSource) {
        this.platformSource = platformSource;
    }

    public void greet() {
        reply("Hello Sir!");
    }

    @Override
    public String name() {
        return platformSource.name();
    }

    @Override
    public Object origin() {
        return platformSource.origin();
    }

    @Override
    public void reply(String message) {
        platformSource.reply(message);
    }

    @Override
    public void warn(String message) {
        platformSource.warn(message);
    }

    @Override
    public void error(String message) {
        platformSource.error(message);
    }

    @Override
    public boolean isConsole() {
        return platformSource.isConsole();
    }
}
