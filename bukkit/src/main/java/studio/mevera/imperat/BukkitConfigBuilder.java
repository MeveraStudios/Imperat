package studio.mevera.imperat;

import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.adventure.CastingAdventure;
import studio.mevera.imperat.adventure.EmptyAdventure;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.*;
import studio.mevera.imperat.exception.selector.InvalidSelectorFieldCriteriaFormat;
import studio.mevera.imperat.exception.selector.UnknownEntitySelectionTypeException;
import studio.mevera.imperat.exception.selector.UnknownSelectorFieldException;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.type.ParameterLocation;
import studio.mevera.imperat.type.ParameterOfflinePlayer;
import studio.mevera.imperat.type.ParameterPlayer;
import studio.mevera.imperat.type.ParameterTargetSelector;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.reflection.Reflections;

public final class BukkitConfigBuilder extends ConfigBuilder<BukkitSource, BukkitImperat, BukkitConfigBuilder> {

    private final static BukkitPermissionResolver DEFAULT_PERMISSION_RESOLVER = new BukkitPermissionResolver();

    private final Plugin plugin;
    private AdventureProvider<CommandSender> adventureProvider;

    private boolean supportBrigadier = false, injectCustomHelp = false;

    BukkitConfigBuilder(Plugin plugin) {
        this.plugin = plugin;
        config.setPermissionResolver(DEFAULT_PERMISSION_RESOLVER);
        addThrowableHandlers();
        registerSourceResolvers();
        registerValueResolvers();
        registerContextResolvers();
    }
    
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<BukkitSource>>() {}.getType(),
                (ctx, paramElement)-> ctx
        );
    }
    
    private void registerSourceResolvers() {
        config.registerSourceResolver(CommandSender.class, BukkitSource::origin);
        config.registerSourceResolver(Player.class, (source) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.asPlayer();
        });
    }

    private void addThrowableHandlers() {
        config.setThrowableResolver(InvalidSelectorFieldCriteriaFormat.class, (ex, imperat, context)-> {
            context.source().error("Invalid field-criteria format '" + ex.getFieldCriteriaInput() + "'");
        });

        config.setThrowableResolver(UnknownSelectorFieldException.class, (ex, imperat, context)-> {
            context.source().error("Unknown selection field '" + ex.getFieldEntered() + "'");
        });

        config.setThrowableResolver(UnknownEntitySelectionTypeException.class, ((exception, imperat, context) -> {
            context.source().error("Unknown selection type '" + exception.getInput() + "'");
        }));

        config.setThrowableResolver(InvalidLocationFormatException.class, (exception, imperat, context) -> {

            InvalidLocationFormatException.Reason reason = exception.getReason();
            String msg = switch (reason) {
                case INVALID_X_COORDINATE -> "Invalid X coordinate '" + exception.getInputX() + "'";
                case INVALID_Y_COORDINATE -> "Invalid Y coordinate '" + exception.getInputY() + "'";
                case INVALID_Z_COORDINATE -> "Invalid Z coordinate '" + exception.getInputZ() + "'";
                case NO_WORLDS_AVAILABLE -> "Failed to fetch the world of the given location";
                case WRONG_FORMAT -> "Wrong location format!";
            };

            context.source().reply("&4Failed to parse location '" + exception.getInput() + "' due to: &c" + msg);
        });

        config.setThrowableResolver(
            UnknownPlayerException.class, (exception, imperat, context) ->
                context.source().error("A player with the name '" + exception.getName() + "' doesn't seem to be online")
        );
        config.setThrowableResolver(
            UnknownOfflinePlayerException.class, (exception, imperat, context) ->
                context.source().error("A player with the name '" + exception.getName() + "' doesn't seem to exist")
        );
        config.setThrowableResolver(
            UnknownWorldException.class, (exception, imperat, context) ->
                context.source().error("A world with the name '" + exception.getName() + "' doesn't seem to exist")
        );

    }

    private void registerValueResolvers() {
        config.registerParamType(Player.class, new ParameterPlayer());
        config.registerParamType(OfflinePlayer.class, new ParameterOfflinePlayer());
        config.registerParamType(Location.class, new ParameterLocation());
        config.registerParamType(TargetSelector.class, new ParameterTargetSelector());
    }

    public void setAdventureProvider(AdventureProvider<CommandSender> adventureProvider) {
        this.adventureProvider = adventureProvider;
    }

    public BukkitConfigBuilder applyBrigadier(boolean supportBrigadier) {
        this.supportBrigadier = supportBrigadier;
        return this;
    }

    public BukkitConfigBuilder injectCustomHelp(boolean injectCustomHelp) {
        this.injectCustomHelp = injectCustomHelp;
        return this;
    }

    @Override
    public @NotNull BukkitImperat build() {
        if (this.adventureProvider == null) {
            this.adventureProvider = this.loadAdventure();
        }
        return new BukkitImperat(plugin, adventureProvider, supportBrigadier, injectCustomHelp, this.config);
    }

    @SuppressWarnings("ConstantConditions")
    private @NotNull AdventureProvider<CommandSender> loadAdventure() {
        if (Reflections.findClass("net.kyori.adventure.audience.Audience")) {
            if (Audience.class.isAssignableFrom(CommandSender.class)) {
                return new CastingAdventure<>();
            } else if (Reflections.findClass("net.kyori.adventure.platform.bukkit.BukkitAudiences")) {
                return new BukkitAdventure(plugin);
            }
        }

        return new EmptyAdventure<>();
    }

}
