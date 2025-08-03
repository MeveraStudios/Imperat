package studio.mevera.imperat;

import net.kyori.adventure.text.ComponentLike;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.context.Source;

import java.util.Objects;
import java.util.UUID;

public class BukkitSource implements Source {

    protected final CommandSender sender;
    protected final AdventureProvider<CommandSender> provider;

    protected BukkitSource(
        final CommandSender sender,
        final AdventureProvider<CommandSender> provider
    ) {
        this.sender = sender;
        this.provider = provider;
    }

    public AdventureProvider<CommandSender> getProvider() {
        return provider;
    }

    /**
     * @return name of a command source
     */
    @Override
    public String name() {
        return sender.getName();
    }

    /**
     * @return The original command sender valueType instance
     */
    @Override
    public CommandSender origin() {
        return sender;
    }

    public Player asPlayer() {
        return as(Player.class);
    }

    /**
     * Replies to the command sender with a string message
     *
     * @param message the message
     */
    @Override
    public void reply(final String message) {
        //check if adventure is loaded, otherwise we send the message normally
        this.sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void warn(String message) {
        reply(ChatColor.YELLOW + message);
    }

    @Override
    public void error(final String message) {
        this.reply(ChatColor.RED + message);
    }

    /**
     * Replies to the command sender with a component message
     *
     * @param component the message component
     */
    public void reply(final ComponentLike component) {
        provider.send(this, component);
    }

    /**
     * @return Whether the command source is from the console
     */
    @Override
    public boolean isConsole() {
        return !(sender instanceof Player);
    }

    @Override
    public UUID uuid() {
        return this.isConsole() ? CONSOLE_UUID : this.asPlayer().getUniqueId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T as(Class<T> clazz) {
        return (T) origin();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BukkitSource source)) return false;
        return Objects.equals(sender, source.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sender);
    }
}
