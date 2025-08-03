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
import studio.mevera.imperat.context.Source;

import java.util.UUID;

public class BungeeSource implements Source {

    private final CommandSender sender;
    private final AdventureProvider<CommandSender> adventureProvider;

    BungeeSource(AdventureProvider<CommandSender> adventureProvider, CommandSender sender) {
        this.adventureProvider = adventureProvider;
        this.sender = sender;
    }

    @Override
    public String name() {
        return sender.getName();
    }

    @Override
    public CommandSender origin() {
        return sender;
    }

    @Override
    public void reply(String message) {
        reply(TextComponent.fromLegacy(message));
    }

    @Override
    public void warn(String message) {
        reply(ChatColor.YELLOW + message);
    }

    @Override
    public void error(final String message) {
        reply(ChatColor.RED + message);
    }

    public void reply(BaseComponent... message) {
        sender.sendMessage(message);
    }

    public void reply(final ComponentLike component) {
        adventureProvider.send(this, component);
    }

    @Override
    public boolean isConsole() {
        return ProxyServer.getInstance().getConsole().equals(sender);
    }

    @Override
    public UUID uuid() {
        return this.isConsole() ? CONSOLE_UUID : this.asPlayer().getUniqueId();
    }

    public @NotNull ProxiedPlayer asPlayer() {
        return (ProxiedPlayer) sender;
    }
}