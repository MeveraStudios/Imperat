package studio.mevera.imperat;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import studio.mevera.imperat.adventure.AdventureHelpComponent;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.context.CommandSource;

public final class BukkitAdventure implements AdventureProvider<CommandSender> {

    private final BukkitAudiences audiences;

    public BukkitAdventure(final Plugin plugin) {
        this.audiences = BukkitAudiences.create(plugin);
    }

    @Override
    public Audience audience(final CommandSender sender) {
        return audiences.sender(sender);
    }

    @Override
    public void close() {
        this.audiences.close();
    }

    @Override
    public <SRC extends CommandSource> AdventureHelpComponent<SRC> createHelpComponent(Component component) {
        return new AdventureHelpComponent<>(component, (source, comp) -> {
            if (source instanceof BukkitCommandSource bukkitSource) {
                bukkitSource.reply(comp);
            } else {
                source.reply(comp.toString());
            }
        });
    }

}
