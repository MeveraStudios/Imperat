package studio.mevera.imperat.bukkit.test;

import org.bukkit.plugin.java.JavaPlugin;
import studio.mevera.imperat.BukkitImperat;

/**
 * A simple test plugin used by MockBukkit for unit testing Imperat commands.
 */
public class TestImperatPlugin extends JavaPlugin {

    private BukkitImperat imperat;

    @Override
    public void onEnable() {
        imperat = BukkitImperat.builder(this)
                          .build();

        // Register the test command
        imperat.registerCommand(new GreetCommand());
    }

    @Override
    public void onDisable() {
        if (imperat != null) {
            imperat.shutdownPlatform();
        }
    }

    public BukkitImperat getImperat() {
        return imperat;
    }
}

