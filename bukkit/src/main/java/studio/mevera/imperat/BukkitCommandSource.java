package studio.mevera.imperat;

import net.kyori.adventure.text.ComponentLike;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.adventure.AdventureCommandSource;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.context.CommandSource;

import java.util.Objects;
import java.util.UUID;

/**
 * A Bukkit-specific implementation of {@link CommandSource} that wraps a Bukkit {@link CommandSender}.
 * This class provides a bridge between Bukkit's command system and the Imperat framework,
 * supporting both legacy Bukkit messaging and modern Adventure API components.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Support for both Player and Console command sources</li>
 *   <li>Adventure API integration for rich text messaging</li>
 *   <li>Legacy color code support for backward compatibility</li>
 *   <li>Type-safe casting to Player or CommandSender</li>
 *   <li>UUID-based identification for players</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * // In a command method
 * @Command("teleport")
 * public void teleport(BukkitCommandSource source, Player target) {
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
 * @see CommandSource
 * @see CommandSender
 */
public class BukkitCommandSource implements AdventureCommandSource {

    protected final CommandSender sender;
    protected final AdventureProvider<CommandSender> provider;

    /**
     * Optional Paper {@code CommandSourceStack} — populated only when this
     * source was produced by the modern-Paper backend through a Brigadier
     * dispatch. Plugins running on legacy Spigot or routed through plain
     * {@code CommandSender} will see {@code null} here. Access via
     * {@link #stack()}.
     *
     * <p>Held as {@link Object} so the field type does not force a class
     * load of {@code io.papermc.paper.command.brigadier.CommandSourceStack}
     * on the legacy classpath.</p>
     */
    protected final @Nullable Object commandSourceStack;

    /**
     * Creates a new BukkitCommandSource wrapping the specified CommandSender.
     * Used by the legacy backend and by source-provider resolvers that
     * lack a Paper {@code CommandSourceStack}.
     *
     * @param sender the Bukkit CommandSender to wrap (player or console)
     * @param provider the Adventure provider for rich text support
     */
    public BukkitCommandSource(
            final CommandSender sender,
            final AdventureProvider<CommandSender> provider
    ) {
        this(sender, provider, null);
    }

    /**
     * Creates a new BukkitCommandSource wrapping a {@code CommandSender}
     * AND its originating Paper {@code CommandSourceStack}. The stack is
     * stored as {@link Object} to keep this class loadable on legacy
     * classpaths where the Paper class is absent.
     */
    public BukkitCommandSource(
            final CommandSender sender,
            final AdventureProvider<CommandSender> provider,
            final @Nullable Object commandSourceStack
    ) {
        this.sender = sender;
        this.provider = provider;
        this.commandSourceStack = commandSourceStack;
    }

    /**
     * Underlying Paper {@code CommandSourceStack}, or {@code null} when
     * this source did not originate from a modern-Paper Brigadier dispatch
     * (i.e. legacy backend, or wrapped from a plain {@code CommandSender}).
     *
     * <p>Returned as {@link Object} so callers on legacy classpaths can
     * safely reference this method without triggering a class-load failure.
     * On modern Paper, cast to
     * {@code io.papermc.paper.command.brigadier.CommandSourceStack}.</p>
     */
    public @Nullable Object stack() {
        return commandSourceStack;
    }

    /**
     * Gets the Adventure provider used for rich text messaging.
     *
     * @return the Adventure provider instance
     */
    public AdventureProvider<CommandSender> getProvider() {
        return provider;
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
     * Gets the original command sender instance.
     * This can be used to access Bukkit's API directly if needed.
     *
     * @return the original CommandSender instance
     */
    @Override
    public CommandSender origin() {
        return sender;
    }

    /**
     * Casts this source to a Player, if possible.
     *
     * @return the Player instance, or null if this source is not a player
     */
    public Player asPlayer() {
        return as(Player.class);
    }

    /**
     * Replies to the command sender with a string message.
     * This method supports legacy color codes (&amp;f) for backward compatibility.
     *
     * @param message the message to send
     */
    @Override
    public void reply(final String message) {
        //check if adventure is loaded, otherwise we send the message normally
        this.sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Sends a warning message to the command sender.
     * Warning messages are prefixed with yellow color.
     *
     * @param message the warning message
     */
    @Override
    public void warn(String message) {
        reply(ChatColor.YELLOW + message);
    }

    /**
     * Sends an error message to the command sender.
     * Error messages are prefixed with red color.
     *
     * @param message the error message
     */
    @Override
    public void error(final String message) {
        this.reply(ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + message);
    }

    /**
     * Replies to the command sender with a component message.
     * This method should be used for sending rich text messages using the Adventure API.
     *
     * @param component the message component
     */
    public void reply(final ComponentLike component) {
        provider.send(this, component);
    }

    /**
     * Checks if the command source is from the console.
     *
     * @return true if this source is from the console, false if from a player
     */
    @Override
    public boolean isConsole() {
        return !(sender instanceof Player);
    }

    /**
     * Gets the UUID of the command source.
     * For console sources, a constant UUID is returned.
     *
     * @return the UUID of the source
     */
    @Override
    public UUID uuid() {
        return this.isConsole() ? CONSOLE_UUID : this.asPlayer().getUniqueId();
    }

    /**
     * Casts the origin command sender to the specified class type.
     * This method is type-safe and avoids the need for explicit casting.
     *
     * @param clazz the class to cast to
     * @param <T> the type parameter
     * @return the casted instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T as(Class<T> clazz) {
        return (T) origin();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BukkitCommandSource source)) {
            return false;
        }
        return Objects.equals(sender, source.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sender);
    }
}
