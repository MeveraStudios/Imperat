package studio.mevera.imperat;

import net.kyori.adventure.text.ComponentLike;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.adventure.AdventureSource;
import studio.mevera.imperat.context.Source;

import java.util.UUID;

/**
 * A BungeeCord-specific implementation of {@link Source} that wraps a BungeeCord {@link CommandSender}.
 * This class provides a bridge between BungeeCord's command system and the Imperat framework,
 * supporting both legacy BungeeCord messaging and modern Adventure API components.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Support for both ProxiedPlayer and Console command sources</li>
 *   <li>Adventure API integration for rich text messaging</li>
 *   <li>Legacy BaseComponent support for backward compatibility</li>
 *   <li>Type-safe casting to ProxiedPlayer or CommandSender</li>
 *   <li>UUID-based identification for players</li>
 *   <li>Cross-server player management</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * // In a command method
 * @Command("send")
 * public void sendToServer(BungeeSource source, ProxiedPlayer target, String serverName) {
 *     if (source.isConsole()) {
 *         source.error("Only players can use this command!");
 *         return;
 *     }
 *     ProxiedPlayer player = source.asPlayer();
 *     // ... server transfer logic
 * }
 * }</pre>
 *
 * @author Imperat Framework
 * @see Source
 * @see CommandSender
 * @see ProxiedPlayer
 * @since 1.0
 */
public class BungeeSource implements AdventureSource {

    private final CommandSender sender;
    private final AdventureProvider<CommandSender> adventureProvider;

    /**
     * Creates a new BungeeSource wrapping the specified CommandSender.
     *
     * @param adventureProvider the Adventure provider for rich text support
     * @param sender            the BungeeCord CommandSender to wrap (player or console)
     */
    BungeeSource(AdventureProvider<CommandSender> adventureProvider, CommandSender sender) {
        this.adventureProvider = adventureProvider;
        this.sender = sender;
    }

    /**
     * Gets the name of this command source.
     *
     * @return the player name if this is a player, or "CONSOLE" if this is the console
     */
    @Override
    public String name() {
        return sender.getName();
    }

    /**
     * Gets the original BungeeCord CommandSender that this BungeeSource wraps.
     *
     * @return the underlying BungeeCord CommandSender
     */
    @Override
    public CommandSender origin() {
        return sender;
    }

    /**
     * Sends a message to this command source.
     *
     * @param message the message to send, in legacy text format
     */
    @Override
    public void reply(String message) {
        reply(TextComponent.fromLegacy(message));
    }

    /**
     * Sends a warning message to this command source.
     *
     * @param message the warning message to send
     */
    @Override
    public void warn(String message) {
        reply(ChatColor.YELLOW + message);
    }

    /**
     * Sends an error message to this command source.
     *
     * @param message the error message to send
     */
    @Override
    public void error(final String message) {
        reply(ChatColor.RED + message);
    }

    /**
     * Sends a message to this command source using BaseComponent arrays.
     *
     * @param message the message components to send
     */
    public void reply(BaseComponent... message) {
        sender.sendMessage(message);
    }

    /**
     * Sends a message to this command source using Adventure API components.
     *
     * @param component the component to send
     */
    public void reply(final ComponentLike component) {
        adventureProvider.send(this, component);
    }

    /**
     * Checks if the command source is the console.
     *
     * @return true if this source is the console, false otherwise
     */
    @Override
    public boolean isConsole() {
        return ProxyServer.getInstance().getConsole().equals(sender);
    }

    /**
     * Gets the UUID of this command source.
     *
     * @return the UUID of the player, or a constant UUID for the console
     */
    @Override
    public UUID uuid() {
        return this.isConsole() ? CONSOLE_UUID : this.asPlayer().getUniqueId();
    }

    /**
     * Gets this command source as a ProxiedPlayer.
     *
     * @return this command source as a ProxiedPlayer
     * @throws ClassCastException if this source is not a ProxiedPlayer
     */
    public @NotNull ProxiedPlayer asPlayer() {
        return (ProxiedPlayer) sender;
    }
}