package studio.mevera.imperat.bukkit.test;

import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Named;

/**
 * A simple test command: /greet [name]
 */
@Command("greet")
public class GreetCommand {

    @Execute
    public void defaultUsage(BukkitSource source) {
        source.reply("Hello, World!");
    }

    @Execute
    public void greetPlayer(BukkitSource source, @Named("name") String name) {
        source.reply("Hello, " + name + "!");
    }
}

