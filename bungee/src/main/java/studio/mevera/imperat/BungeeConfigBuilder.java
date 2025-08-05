package studio.mevera.imperat;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.adventure.BungeeAdventure;
import studio.mevera.imperat.adventure.EmptyAdventure;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.resolvers.BungeePermissionChecker;
import studio.mevera.imperat.type.ParameterProxiedPlayer;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.reflection.Reflections;

public final class BungeeConfigBuilder extends ConfigBuilder<BungeeSource, BungeeImperat, BungeeConfigBuilder> {

    private final static BungeePermissionChecker DEFAULT_PERMISSION_RESOLVER = new BungeePermissionChecker();
    private final Plugin plugin;

    private AdventureProvider<CommandSender> adventureProvider;

    BungeeConfigBuilder(Plugin plugin, @Nullable AdventureProvider<CommandSender> adventureProvider) {
        this.plugin = plugin;
        config.setPermissionResolver(DEFAULT_PERMISSION_RESOLVER);
        addThrowableHandlers();
        registerSourceResolvers();
        registerValueResolvers();
        registerContextResolvers();
    }
    
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<BungeeSource>>() {}.getType(),
                (ctx, paramElement)-> ctx
        );
    }

    public void setAdventureProvider(AdventureProvider<CommandSender> adventureProvider) {
        this.adventureProvider = adventureProvider;
    }

    private AdventureProvider<CommandSender> loadAdventure() {
        if (Reflections.findClass(() -> BungeeAudiences.class)) {
            return new BungeeAdventure(plugin);
        }
        return new EmptyAdventure<>();
    }

    private void registerSourceResolvers() {
        config.registerSourceResolver(CommandSender.class, BungeeSource::origin);
        config.registerSourceResolver(ProxiedPlayer.class, (source) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.asPlayer();
        });
    }

    private void addThrowableHandlers() {
        config.setThrowableResolver(
            UnknownPlayerException.class, (exception, imperat, context) ->
                context.source().error("A player with the name '" + exception.getName() + "' doesn't seem to be online")
        );
    }

    private void registerValueResolvers() {
        config.registerParamType(ProxiedPlayer.class, new ParameterProxiedPlayer());
    }

    @Override
    public @NotNull BungeeImperat build() {
        if (this.adventureProvider == null) {
            this.adventureProvider = this.loadAdventure();
        }
        return new BungeeImperat(plugin, adventureProvider, this.config);
    }
}