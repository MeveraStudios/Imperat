package studio.mevera.imperat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import studio.mevera.imperat.adventure.AdventureSource;
import studio.mevera.imperat.context.Source;

import java.util.UUID;

/**
 * A Minestom-specific implementation of {@link Source} that wraps a Minestom {@link CommandSender}.
 * This class provides a bridge between Minestom's command system and the Imperat framework,
 * with full Adventure API integration for rich text messaging.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Support for both Player and Console command sources</li>
 *   <li>Native Adventure API integration for rich text messaging</li>
 *   <li>Type-safe casting to Player or ConsoleSender</li>
 *   <li>UUID-based identification for players</li>
 *   <li>Modern Minecraft server support with full component messaging</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * // In a command method
 * @Command("teleport")
 * public void teleport(MinestomSource source, Player target) {
 *     if (source.isConsole()) {
 *         source.error("Only players can teleport!");
 *         return;
 *     }
 *     Player player = source.asPlayer();
 *     // ... teleport logic
 * }
 * }</pre>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see Source
 * @see CommandSender
 * @see Player
 */
public final class MinestomSource implements AdventureSource {
    private final CommandSender sender;

    /**
     * Creates a new MinestomSource wrapping the specified CommandSender.
     *
     * @param sender the Minestom CommandSender to wrap (player or console)
     */
    MinestomSource(CommandSender sender) {
        this.sender = sender;
    }

    /**
     * Gets the name of this command source.
     *
     * @return the username if this is a player, or "CONSOLE" if this is the console
     */
    @Override
    public String name() {
        return sender instanceof Player player ? player.getUsername() : "CONSOLE";
    }

    /**
     * Gets the original Minestom CommandSender that this MinestomSource wraps.
     *
     * @return the underlying Minestom CommandSender
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

    /**
     * Sends a message to this command source using Adventure components.
     *
     * @param componentLike the component to send
     */
    @Override
    public void reply(ComponentLike componentLike) {
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
