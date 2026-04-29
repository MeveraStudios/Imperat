package studio.mevera.imperat.paper;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.adventure.AdventureCommandSource;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.context.CommandSource;

import java.util.Objects;
import java.util.UUID;

/**
 * Paper-modern (1.21.4+) {@link CommandSource} backed by a Paper
 * {@link CommandSourceStack}. Unlike BukkitCommandSource
 * which wraps a raw {@code CommandSender}, this source preserves the
 * full {@code CommandSourceStack} so command bodies can introspect the
 * {@code /execute}-context: actual sender, executing entity (may differ
 * from sender), execution location, and rotation.
 *
 * <p>Convenience accessors mirror the Bukkit source's API for plugins
 * that just want a {@link CommandSender} or {@link Player} reference.</p>
 *
 * @since 4.0.0 (Paper module)
 */
public class PaperCommandSource implements AdventureCommandSource {

    private final @Nullable CommandSourceStack stack;
    private final @NotNull CommandSender sender;
    private final @NotNull AdventureProvider<CommandSender> provider;

    PaperCommandSource(@NotNull CommandSourceStack stack,
            @NotNull AdventureProvider<CommandSender> provider) {
        this.stack = stack;
        this.sender = stack.getSender();
        this.provider = provider;
    }

    PaperCommandSource(@NotNull CommandSender sender,
            @NotNull AdventureProvider<CommandSender> provider) {
        this.stack = null;
        this.sender = sender;
        this.provider = provider;
    }

    /**
     * Underlying Paper command-source stack — preserves {@code /execute}
     * context when this source was produced from a Brigadier dispatch.
     * Returns {@code null} for headless / non-Brigadier invocations.
     */
    public @Nullable CommandSourceStack stack() {
        return stack;
    }

    public AdventureProvider<CommandSender> getProvider() {
        return provider;
    }

    @Override
    public String name() {
        return sender.getName();
    }

    @Override
    public CommandSender origin() {
        return sender;
    }

    public Player asPlayer() {
        return as(Player.class);
    }

    @Override
    public void reply(final String message) {
        sender.sendRichMessage(message);
    }

    @Override
    public void warn(String message) {
        reply("<gold>WARNING:</gold> " + message);
    }

    @Override
    public void error(final String message) {
        this.reply("<dark_red>ERROR:</dark_red> <red>" + message + "</red>");
    }

    public void reply(final ComponentLike component) {
        provider.send(this, component);
    }

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
        if (!(o instanceof PaperCommandSource source)) {
            return false;
        }
        return Objects.equals(sender, source.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sender);
    }
}
