package studio.mevera.imperat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import studio.mevera.imperat.context.Source;

import java.util.UUID;

public final class MinestomSource implements Source {
    private final CommandSender sender;

    MinestomSource(CommandSender sender) {
        this.sender = sender;
    }

    /**
     * @return name of a command source
     */
    @Override
    public String name() {
        return sender instanceof Player player ? player.getUsername() : "CONSOLE";
    }

    /**
     * @return The original command sender valueType instance
     */
    @Override
    public CommandSender origin() {
        return sender;
    }

    /**
     * Replies to the command sender with a string message
     *
     * @param message the message
     */
    @Override
    public void reply(String message) {
        sender.sendMessage(message);
    }

    /**
     * Replies to the command sender with a string message
     * formatted specifically for error messages
     *
     * @param message the message
     */
    @Override
    public void warn(String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.YELLOW));
    }

    /**
     * Replies to the command sender with a string message
     * formatted specifically for error messages
     *
     * @param message the message
     */
    @Override
    public void error(String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    public void sendMessage(ComponentLike componentLike) {
        sender.sendMessage(componentLike);
    }

    /**
     * @return Whether the command source is from the console
     */
    @Override
    public boolean isConsole() {
        return sender instanceof ConsoleSender;
    }

    @Override
    public UUID uuid() {
        return this.isConsole() ? CONSOLE_UUID : this.asPlayer().getUuid();
    }

    public Player asPlayer() {
        return as(Player.class);
    }

}
