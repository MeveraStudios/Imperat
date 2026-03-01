package studio.mevera.imperat.bukkit.test.commands;

import org.bukkit.entity.Player;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Suggest;

/**
 * A simple test command: /greet [name]
 */
@RootCommand({"tell", "msg", "t", "w", "whisper", "pm"})
public class TellCmd {

    @Execute
    public void whisper(Player source, @Named("name") @Suggest({"Mazen", "Ahmed", "Eyad"}) String name, @Named("message") @Greedy String message) {
        source.sendMessage("Message from " + source.getName() + " to " + name + ": " + message);
    }
}

