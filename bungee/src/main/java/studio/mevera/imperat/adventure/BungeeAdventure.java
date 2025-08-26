package studio.mevera.imperat.adventure;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import studio.mevera.imperat.BungeeSource;
import studio.mevera.imperat.context.Source;

public final class BungeeAdventure implements AdventureProvider<CommandSender> {

    private final BungeeAudiences audiences;

    public BungeeAdventure(final Plugin plugin) {
        this.audiences = BungeeAudiences.create(plugin);
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
    public <SRC extends Source> AdventureHelpComponent<SRC> createHelpComponent(Component component) {
        return new AdventureHelpComponent<>(component, (source, comp) -> {
            if (source instanceof BungeeSource bungeeSource) {
                bungeeSource.reply(comp);
            } else {
                source.reply(comp.toString());
            }
        });
    }
    
}
