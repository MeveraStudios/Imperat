package studio.mevera.imperat.bukkit.test;

import org.bukkit.entity.Player;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Suggest;

/**
 * A simple test command: /greet [name]
 */
@Command({"tell", "msg", "t", "w", "whisper", "pm"})
public class TellCmd {

    @Execute
    public void defaultUsage(Player source) {
        source.sendMessage("/tell <name> - Greet someone by name.");
    }

    @Execute
    public void whisper(Player source, @Named("name") @Suggest({"Mazen", "Ahmed", "Eyad"}) String name, @Named("message") @Greedy String message) {
        source.sendMessage("Message from " + source.getName() + " to " + name + ": " + message);
    }
}

