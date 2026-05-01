package studio.mevera.imperat;

import net.kyori.adventure.audience.Audience;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.adventure.CastingAdventure;
import studio.mevera.imperat.adventure.EmptyAdventure;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.responses.BukkitResponseKey;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.reflection.Reflections;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration builder for BukkitImperat instances.
 * This builder provides a fluent API for configuring and customizing the behavior
 * of Imperat commands in a Bukkit/Spigot/Paper environment.
 *
 * <p>The builder automatically sets up backend-agnostic defaults:</p>
 * <ul>
 *   <li>Exception handlers for common Bukkit scenarios</li>
 *   <li>CommandSource resolvers for type-safe command source handling</li>
 *   <li>Adventure API integration with automatic detection</li>
 *   <li>Permission system integration</li>
 * </ul>
 *
 * <p>Backend-specific defaults (parameter types, Brigadier wiring) are applied
 * by the chosen {@link studio.mevera.imperat.backend.BukkitBackend} during
 * {@code BukkitImperat} construction — modern Paper installs Paper-native
 * argument types with client-side suggestions, legacy installs the existing
 * name-based bukkit types.</p>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * BukkitImperat imperat = BukkitImperat.builder(plugin).build();
 * }</pre>
 *
 * @author Imperat Framework
 * @see BukkitImperat
 * @since 1.0
 */
public class BukkitConfigBuilder<S extends BukkitCommandSource>
        extends ConfigBuilder<S, BukkitImperat<S>, BukkitConfigBuilder<S>> {

    private final static BukkitPermissionChecker<?> DEFAULT_PERMISSION_RESOLVER = new BukkitPermissionChecker<>();

    private final Plugin plugin;
    private AdventureProvider<CommandSender> adventureProvider;
    private boolean setOverrideBrigadierMessaging = true;

    @SuppressWarnings({"unchecked", "rawtypes"}) BukkitConfigBuilder(Plugin plugin, Class<S> sourceClass,
            CommandSourceMapper<BukkitCommandSource, S> mapper) {
        super(sourceClass);
        this.plugin = plugin;
        config.setSourceMapper(mapper);
        config.setPermissionResolver((BukkitPermissionChecker) DEFAULT_PERMISSION_RESOLVER);
        registerBukkitResponses();
        registerSourceResolvers();
        registerContextResolvers();
        config.setDefaultSuggestionProvider(
                (context, argument) -> {
                    BukkitCommandSource source = context.source();
                    List<String> onlinePlayers = new ArrayList<>();
                    final Player player = source.isConsole() ? null : source.asPlayer();
                    for (final Player online : source.origin().getServer().getOnlinePlayers()) {
                        final String name = online.getName();
                        if ((player == null || player.canSee(online)) && name.startsWith(context.getArgToComplete().value())) {
                            onlinePlayers.add(name);
                        }
                    }
                    return onlinePlayers;
                }
        );
    }

    private void registerContextResolvers() {
        // Type-literal-keyed registrations parameterized over S MUST be
        // deferred until build() — the mapper / sourceClass might still
        // change before the user calls build(). Building the type literal
        // eagerly here would freeze it to the wrong S.
        deferredDefaults.add(cfg -> {
            cfg.registerContextArgumentProvider(
                    TypeWrap.ofParameterized(ExecutionContext.class, sourceClass).getType(),
                    (ctx, paramElement) -> ctx
            );
            cfg.registerContextArgumentProvider(
                    TypeWrap.ofParameterized(CommandHelp.class, sourceClass).getType(),
                    (ctx, paramElement) -> CommandHelp.create(ctx)
            );
        });

        // Plugin / Server are raw-class-keyed — no S parametrization, eager is fine
        config.registerContextArgumentProvider(Plugin.class, (ctx, paramElement) -> plugin);
        config.registerContextArgumentProvider(Server.class, (ctx, paramElement) -> plugin.getServer());
    }

    private void registerSourceResolvers() {
        // v4: SourceProviderRegistry deleted. Cross-source-type @Execute
        // params resolve via ExecutionContextImpl.provideSource(Type) ->
        // assignability against the canonical S + its origin(). With
        // S extends BukkitCommandSource, declaring `Player` / `CommandSender`
        // / `ConsoleCommandSender` parameters works automatically because
        // they're assignable from `s.origin()` (or s itself for
        // CommandSender). Domain-specific `@Execute void cmd(Player p)`
        // throws when `s` is console — that gating moves to the
        // ContextArgumentProvider below.
        config.registerContextArgumentProvider(Player.class, (ctx, p) -> {
            BukkitCommandSource s = ctx.source();
            if (s.isConsole()) {
                throw ResponseException.of(BukkitResponseKey.ONLY_PLAYER);
            }
            return s.asPlayer();
        });
        config.registerContextArgumentProvider(ConsoleCommandSender.class, (ctx, p) -> {
            var origin = ctx.source().origin();
            if (!(origin instanceof ConsoleCommandSender console)) {
                throw ResponseException.of(BukkitResponseKey.ONLY_CONSOLE);
            }
            return console;
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

    public BukkitConfigBuilder<S> setAdventureProvider(AdventureProvider<CommandSender> adventureProvider) {
        this.adventureProvider = adventureProvider;
        return this;
    }

    /**
     * Whether the modern Paper backend should listen for {@code UnknownCommandEvent}
     * and reroute hidden-by-permissions cases through Imperat's exception pipeline.
     * No effect on the legacy backend. Default: {@code true}.
     */
    public BukkitConfigBuilder<S> setOverrideBrigadierMessaging(boolean enabled) {
        this.setOverrideBrigadierMessaging = enabled;
        return this;
    }

    @Override
    public @NotNull BukkitImperat<S> build() {
        if (this.adventureProvider == null) {
            this.adventureProvider = this.loadAdventure();
        }
        // Drain deferred defaults AFTER mapper is set — the mapper might
        // have been swapped by the user's `.source(...)` call.
        materializeDeferredDefaults();
        return new BukkitImperat<>(plugin, adventureProvider, this.config, setOverrideBrigadierMessaging);
    }

    @SuppressWarnings("ConstantConditions")
    private @NotNull AdventureProvider<CommandSender> loadAdventure() {
        if (Reflections.findClass("net.kyori.adventure.audience.Audience")) {
            if (Audience.class.isAssignableFrom(CommandSender.class)) {
                return new CastingAdventure<>() {
                };
            } else if (Reflections.findClass("net.kyori.adventure.platform.bukkit.BukkitAudiences")) {
                return new BukkitAdventure(plugin);
            }
        }

        return new EmptyAdventure<>();
    }

}
