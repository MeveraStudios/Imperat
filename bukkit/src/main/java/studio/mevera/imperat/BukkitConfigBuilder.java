package studio.mevera.imperat;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.adventure.AdventureHelpComponent;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.adventure.AdventureSource;
import studio.mevera.imperat.adventure.CastingAdventure;
import studio.mevera.imperat.adventure.EmptyAdventure;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.InvalidLocationFormatException;
import studio.mevera.imperat.exception.OnlyConsoleAllowedException;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.exception.UnknownOfflinePlayerException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.exception.UnknownWorldException;
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

/**
 * Configuration builder for BukkitImperat instances.
 * This builder provides a fluent API for configuring and customizing the behavior
 * of Imperat commands in a Bukkit/Spigot/Paper environment.
 *
 * <p>The builder automatically sets up:</p>
 * <ul>
 *   <li>Bukkit-specific parameter types (Player, Location, OfflinePlayer, TargetSelector)</li>
 *   <li>Exception handlers for common Bukkit scenarios</li>
 *   <li>Source resolvers for type-safe command source handling</li>
 *   <li>Adventure API integration with automatic detection</li>
 *   <li>Entity selector support (@p, @a, @e, @r)</li>
 *   <li>Permission system integration</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * BukkitImperat imperat = BukkitImperat.builder(plugin)
 *     .applyBrigadier(true)  // Enable Brigadier for Paper
 *     .build();
 * }</pre>
 *
 * @author Imperat Framework
 * @see BukkitImperat
 * @since 1.0
 */
public final class BukkitConfigBuilder extends ConfigBuilder<BukkitSource, BukkitImperat, BukkitConfigBuilder> {

    private final static BukkitPermissionChecker DEFAULT_PERMISSION_RESOLVER = new BukkitPermissionChecker();

    private final Plugin plugin;
    private AdventureProvider<CommandSender> adventureProvider;

    private boolean supportBrigadier = false;

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
                (ctx, paramElement) -> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<BukkitSource>>() {}.getType(),
                (ctx, paramElement) -> CommandHelp.create(ctx)
        );

        // Enhanced context resolvers similar to Velocity
        config.registerContextResolver(Plugin.class, (ctx, paramElement) -> plugin);
        config.registerContextResolver(Server.class, (ctx, paramElement) -> plugin.getServer());
    }

    private void registerSourceResolvers() {
        config.registerSourceResolver(AdventureSource.class, (bukkitSource, ctx) -> bukkitSource);
        config.registerSourceResolver(CommandSender.class, (bukkitSource, ctx) -> bukkitSource.origin());
        config.registerSourceResolver(ConsoleCommandSender.class, (bukkitSource, ctx) -> {
            var origin = bukkitSource.origin();
            if (!(origin instanceof ConsoleCommandSender console)) {
                throw new OnlyConsoleAllowedException();
            }
            return console;
        });

        config.registerSourceResolver(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.asPlayer();
        });
    }

    private void addThrowableHandlers() {
        config.setThrowableResolver(OnlyPlayerAllowedException.class, (ex, context) ->
                context.source().error("Only players can do this!"));

        config.setThrowableResolver(
                OnlyConsoleAllowedException.class,
                (ex, context) -> context.source().error("Only console can do this!")
        );

        config.setThrowableResolver(InvalidSelectorFieldCriteriaFormat.class, (ex, context) ->
                context.source().error("Invalid field-criteria format '" + ex.getFieldCriteriaInput() + "'"));

        config.setThrowableResolver(
                UnknownSelectorFieldException.class,
                (ex, context) ->
                        context.source().error("Unknown selection field '" + ex.getFieldEntered() + "'")
        );

        config.setThrowableResolver(
                UnknownEntitySelectionTypeException.class,
                (exception, context) ->
                        context.source().error("Unknown selection type '" + exception.getInput() + "'")
        );

        config.setThrowableResolver(InvalidLocationFormatException.class, (exception, context) -> {
            InvalidLocationFormatException.Reason reason = exception.getReason();
            String msg = switch (reason) {
                case INVALID_X_COORDINATE -> "Invalid X coordinate '" + exception.getInputX() + "'";
                case INVALID_Y_COORDINATE -> "Invalid Y coordinate '" + exception.getInputY() + "'";
                case INVALID_Z_COORDINATE -> "Invalid Z coordinate '" + exception.getInputZ() + "'";
                case INVALID_YAW_COORDINATE -> "Invalid Yaw coordinate '" + exception.getInputYaw() + "'";
                case INVALID_PITCH_COORDINATE -> "Invalid Pitch coordinate '" + exception.getInputPitch() + "'";
                case NO_WORLDS_AVAILABLE -> "Failed to fetch the world of the given location";
                case WRONG_FORMAT -> "Wrong location format!";
                case SELF_LOCATION_NOT_AVAILABLE -> null;
            };

            context.source().reply("&4Failed to parse location '" + exception.getInput() + "' due to: &c" + msg);
        });

        config.setThrowableResolver(
                UnknownPlayerException.class,
                (exception, context) ->
                        context.source().error("A player with the name '" + exception.getName() + "' doesn't seem to be online")
        );
        config.setThrowableResolver(
                UnknownOfflinePlayerException.class,
                (exception, context) ->
                        context.source().error("A player with the name '" + exception.getName() + "' doesn't seem to exist")
        );
        config.setThrowableResolver(
                UnknownWorldException.class,
                (exception, context) ->
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

    @Override
    public @NotNull BukkitImperat build() {
        if (this.adventureProvider == null) {
            this.adventureProvider = this.loadAdventure();
        }
        return new BukkitImperat(plugin, adventureProvider, supportBrigadier, this.config);
    }

    @SuppressWarnings("ConstantConditions")
    private @NotNull AdventureProvider<CommandSender> loadAdventure() {
        if (Reflections.findClass("net.kyori.adventure.audience.Audience")) {
            if (Audience.class.isAssignableFrom(CommandSender.class)) {
                return new CastingAdventure<>() {
                    @Override
                    public <SRC extends Source> AdventureHelpComponent<SRC> createHelpComponent(Component component) {
                        return new AdventureHelpComponent<>(component, (source, comp) -> {
                            if (source instanceof BukkitSource bukkitSource) {
                                bukkitSource.reply(comp);
                            } else {
                                source.reply(comp.toString());
                            }
                        });
                    }
                };
            } else if (Reflections.findClass("net.kyori.adventure.platform.bukkit.BukkitAudiences")) {
                return new BukkitAdventure(plugin);
            }
        }

        return new EmptyAdventure<>();
    }

}
