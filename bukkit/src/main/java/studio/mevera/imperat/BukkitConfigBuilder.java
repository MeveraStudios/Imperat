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
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.BukkitResponseKey;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.type.LocationArgument;
import studio.mevera.imperat.type.OfflinePlayerArgument;
import studio.mevera.imperat.type.PlayerArgument;
import studio.mevera.imperat.type.TargetSelectorArgument;
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
        registerBukkitResponses();
        registerSourceResolvers();
        registerValueResolvers();
        registerContextResolvers();
    }

    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<BukkitSource>>() {
                }.getType(),
                (ctx, paramElement) -> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<BukkitSource>>() {
                }.getType(),
                (ctx, paramElement) -> CommandHelp.create(ctx)
        );

        // Enhanced context resolvers similar to Velocity
        config.registerContextResolver(Plugin.class, (ctx, paramElement) -> plugin);
        config.registerContextResolver(Server.class, (ctx, paramElement) -> plugin.getServer());
    }

    private void registerSourceResolvers() {
        config.registerSourceProvider(AdventureSource.class, (bukkitSource, ctx) -> bukkitSource);
        config.registerSourceProvider(CommandSender.class, (bukkitSource, ctx) -> bukkitSource.origin());
        config.registerSourceProvider(ConsoleCommandSender.class, (bukkitSource, ctx) -> {
            var origin = bukkitSource.origin();
            if (!(origin instanceof ConsoleCommandSender console)) {
                throw new CommandException(BukkitResponseKey.ONLY_CONSOLE);
            }
            return console;
        });

        config.registerSourceProvider(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new CommandException(BukkitResponseKey.ONLY_PLAYER);
            }
            return source.asPlayer();
        });
    }

    private void registerBukkitResponses() {
        this.visit(ImperatConfig::getResponseRegistry, registry -> {
            registry.registerResponse(BukkitResponseKey.ONLY_PLAYER, () -> "Only players can do this!");
            registry.registerResponse(BukkitResponseKey.ONLY_CONSOLE, () -> "Only console can do this!");

            registry.registerResponse(BukkitResponseKey.UNKNOWN_PLAYER, () -> "A player with the name '%input%' doesn't seem to be online", "input");
            registry.registerResponse(BukkitResponseKey.UNKNOWN_OFFLINE_PLAYER, () -> "A player with the name '%input%' doesn't seem to exist",
                    "input");
            registry.registerResponse(BukkitResponseKey.UNKNOWN_WORLD, () -> "A world with the name '%input%' doesn't seem to exist", "input");
            registry.registerResponse(BukkitResponseKey.INVALID_LOCATION, () -> "&4Failed to parse location '%input%' due to: &c%cause%", "input",
                    "inputX", "inputY", "inputZ", "inputYaw", "inputPitch", "cause");


            registry.registerResponse(BukkitResponseKey.INVALID_SELECTOR_FIELD, () -> "Invalid field-criteria format '%criteria_entered%'", "input",
                    "criteria_expression");
            registry.registerResponse(BukkitResponseKey.UNKNOWN_SELECTOR_FIELD, () -> "Unknown selection field '%field_entered%'", "input",
                    "field_entered");
            registry.registerResponse(BukkitResponseKey.UNKNOWN_SELECTION_TYPE, () -> "Unknown selection type '%type_entered%'", "input",
                    "type_entered");

        });
    }

    private void registerValueResolvers() {
        config.registerArgType(Player.class, new PlayerArgument());
        config.registerArgType(OfflinePlayer.class, new OfflinePlayerArgument());
        config.registerArgType(Location.class, new LocationArgument());
        config.registerArgType(TargetSelector.class, new TargetSelectorArgument());
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
