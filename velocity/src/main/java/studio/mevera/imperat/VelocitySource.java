package studio.mevera.imperat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.ComponentLike;
import studio.mevera.imperat.adventure.AdventureSource;
import studio.mevera.imperat.context.Source;

import java.util.UUID;

/**
 * A Velocity-specific implementation of {@link Source} that wraps a Velocity {@link CommandSource}.
 * This class provides a bridge between Velocity's command system and the Imperat framework,
 * allowing commands to work seamlessly with Velocity's player and console command sources.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Support for both player and console command sources</li>
 *   <li>Adventure text component support for rich messaging</li>
 *   <li>Type-safe casting to Player or ConsoleCommandSource</li>
 *   <li>UUID-based identification for players</li>
 * </ul>
 *
 * @since 1.0
 * @author Imperat Framework
 */
public class VelocitySource implements AdventureSource {

    private final CommandSource origin;

    /**
     * Creates a new VelocitySource wrapping the specified CommandSource.
     *
     * @param origin the Velocity CommandSource to wrap (player or console)
     */
    VelocitySource(CommandSource origin) {
        this.origin = origin;
    }

    /**
     * Gets the name of this command source.
     *
     * @return the username if this is a player, or "CONSOLE" if this is the console
     */
    @Override
    public String name() {
        return origin instanceof Player pl ? pl.getUsername() : "CONSOLE";
    }

    /**
     * Gets the original Velocity CommandSource that this VelocitySource wraps.
     *
     * @return the underlying Velocity CommandSource
     */
    @Override
    public CommandSource origin() {
        return origin;
    }

    /**
     * Sends a rich message to this command source using Velocity's rich message format.
     * Supports MiniMessage format for styling and colors.
     *
     * @param message the message to send, can include MiniMessage formatting
     */
    @Override
    public void reply(String message) {
        origin.sendRichMessage(message);
    }

    /**
     * Sends an Adventure Component to this command source.
     *
     * @param component the Adventure component to send
     */
    public void reply(ComponentLike component) {
        origin.sendMessage(component);
    }

    /**
     * Sends a warning message to this command source.
     * The message will be formatted in yellow.
     *
     * @param message the warning message to send
     */
    @Override
    public void warn(String message) {
        origin.sendRichMessage("<yellow>" + message + "</yellow>");
    }

    /**
     * Sends an error message to this command source.
     * The message will be formatted in red.
     *
     * @param message the error message to send
     */
    @Override
    public void error(String message) {
        origin.sendRichMessage("<red>" + message + "</red>");
    }

    /**
     * Checks if this command source is the console.
     *
     * @return true if this is the console, false otherwise
     */
    @Override
    public boolean isConsole() {
        return origin instanceof ConsoleCommandSource;
    }

    /**
     * Gets the UUID of this command source.
     * For the console, a predefined constant UUID is returned.
     *
     * @return the UUID of the command source
     */
    @Override
    public UUID uuid() {
        return this.isConsole() ? CONSOLE_UUID : this.asPlayer().getUniqueId();
    }

    /**
     * Casts this command source to a ConsoleCommandSource.
     * This is only safe if {@link #isConsole()} returns true.
     *
     * @return this command source as a ConsoleCommandSource
     * @throws ClassCastException if this is not the console
     */
    public ConsoleCommandSource asConsole() {
        return (ConsoleCommandSource) origin;
    }

    /**
     * Casts this command source to a Player.
     * This is only safe if {@link #isConsole()} returns false.
     *
     * @return this command source as a Player
     * @throws ClassCastException if this is the console
     */
    public Player asPlayer() {
        return (Player) origin;
    }
}
