package studio.mevera.imperat.examples;

import org.bukkit.entity.Player;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Execute;

@Command({"message", "msg"})
@Permission("server.command.message")
class MsgCommand {

    @Execute
    public void defaultUsage(BukkitSource sender) {
        //what happens here is executed when the sender executes the command without any input arguments.
        //also aka: 'Default Command Usage'
        sender.reply("/msg <target> <message>");
    }

    @Execute
    public void mainUsage(BukkitSource sender, Player player, @Greedy String message) {
        String fullMsg = sender.name() + "-> " + player.getName() + ": " + message;
        sender.reply(fullMsg);
        player.sendMessage(fullMsg);
    }

}
