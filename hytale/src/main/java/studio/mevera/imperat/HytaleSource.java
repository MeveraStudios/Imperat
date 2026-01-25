package studio.mevera.imperat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import studio.mevera.imperat.context.Source;

import java.awt.Color;
import java.util.UUID;

public class HytaleSource implements Source {

    private final CommandSender sender;

    HytaleSource(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public String name() {
        return sender.getDisplayName();
    }

    @Override
    public CommandSender origin() {
        return sender;
    }

    @Override
    public void reply(String message) {
        sender.sendMessage(Message.raw(message));
    }

    public void reply(Message message) {
        sender.sendMessage(message);
    }

    @Override
    public void warn(String message) {
        sender.sendMessage(Message.raw(message).color(Color.YELLOW));
    }

    @Override
    public void error(String message) {
        sender.sendMessage(Message.raw(message).color(Color.RED));
    }

    @Override
    public boolean isConsole() {
        return sender instanceof ConsoleSender;
    }

    @Override
    public UUID uuid() {
        return sender.getUuid();
    }

    public Player asPlayer() {
        return as(Player.class);
    }

    public PlayerRef asPlayerRef() {
        return Universe.get().getPlayer(sender.getUuid());
    }

}
