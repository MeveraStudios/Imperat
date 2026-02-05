package studio.mevera.imperat.examples;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.type.PlayerArgument;

class ExamplePlugin extends JavaPlugin {

    private BukkitImperat imperat;

    @Override
    public void onEnable() {
        // Setting up our imperat
        imperat = BukkitImperat.builder(this)
                          .ArgumentType(Player.class, new PlayerArgument())
                          .build();

        // Registering rank command.
        imperat.registerCommand(new MsgCommand());
    }

}