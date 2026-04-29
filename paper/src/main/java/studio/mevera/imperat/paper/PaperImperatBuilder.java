package studio.mevera.imperat.paper;

import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ConfigBuilder;
import studio.mevera.imperat.ResolverRegistrar;
import studio.mevera.imperat.adventure.AdventureCommandSource;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.adventure.CastingAdventure;
import studio.mevera.imperat.adventure.EmptyAdventure;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.paper.argument.PaperArgumentMappings;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.reflection.Reflections;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration builder for {@link PaperImperat}. Registers the Paper-side
 * defaults (source providers, context providers, Paper-friendly argument
 * types, response keys) before constructing the imperat instance.
 *
 * <p>Mirrors the shape of the legacy {@code BukkitConfigBuilder} but
 * targets Paper's modern Brigadier API exclusively — there is no
 * "applyBrigadier" toggle because Brigadier integration is built-in
 * and unconditional on this path.</p>
 *
 * @since 4.0.0 (Paper module)
 */
public final class PaperImperatBuilder extends ConfigBuilder<PaperCommandSource, PaperImperat, PaperImperatBuilder> {

    private final Plugin plugin;
    private AdventureProvider<CommandSender> adventureProvider;
    private boolean overrideBrigadierMessaging = true;

    PaperImperatBuilder(@NotNull Plugin plugin) {
        this.plugin = plugin;
        config.setPermissionResolver((source, permission) -> {
            if (permission == null || permission.isEmpty()) {
                return true;
            }
            return source.origin().hasPermission(permission);
        });

        registerPaperResponses();
        registerSourceResolvers();
        registerContextResolvers();
        PaperArgumentMappings.applyDefaults(config);

        config.setDefaultSuggestionProvider(
                (context, argument) -> {
                    PaperCommandSource source = context.source();
                    List<String> onlinePlayers = new ArrayList<>();
                    Player player = source.isConsole() ? null : source.asPlayer();
                    for (Player online : source.origin().getServer().getOnlinePlayers()) {
                        String name = online.getName();
                        if ((player == null || player.canSee(online))
                                    && name.startsWith(context.getArgToComplete().value())) {
                            onlinePlayers.add(name);
                        }
                    }
                    return onlinePlayers;
                }
        );
    }

    private void registerSourceResolvers() {
        config.registerSourceProvider(AdventureCommandSource.class, (paperSource, ctx) -> paperSource);
        config.registerSourceProvider(CommandSender.class, (paperSource, ctx) -> paperSource.origin());
        config.registerSourceProvider(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw ResponseException.of(PaperResponseKey.ONLY_PLAYER);
            }
            return source.asPlayer();
        });
    }

    private void registerContextResolvers() {
        config.registerContextArgumentProvider(
                new TypeWrap<ExecutionContext<PaperCommandSource>>() {
                }.getType(),
                (ctx, paramElement) -> ctx
        );
        config.registerContextArgumentProvider(
                new TypeWrap<CommandHelp<PaperCommandSource>>() {
                }.getType(),
                (ctx, paramElement) -> CommandHelp.create(ctx)
        );
        config.registerContextArgumentProvider(Plugin.class, (ctx, paramElement) -> plugin);
    }

    private void registerPaperResponses() {
        this.visit(ResolverRegistrar::getResponseRegistry, registry -> {
            registry.registerResponse(PaperResponseKey.ONLY_PLAYER, () -> "Only players can do this!");
            registry.registerResponse(PaperResponseKey.ONLY_CONSOLE, () -> "Only console can do this!");
            registry.registerResponse(PaperResponseKey.UNKNOWN_PLAYER,
                    () -> "A player with the name '%input%' doesn't seem to be online", "input");
            registry.registerResponse(PaperResponseKey.UNKNOWN_OFFLINE_PLAYER,
                    () -> "A player with the name '%input%' doesn't seem to exist", "input");
            registry.registerResponse(PaperResponseKey.UNKNOWN_WORLD,
                    () -> "A world with the name '%input%' doesn't seem to exist", "input");
            registry.registerResponse(PaperResponseKey.INVALID_LOCATION,
                    () -> "&4Failed to parse location '%input%' due to: &c%cause%",
                    "input", "inputX", "inputY", "inputZ", "inputYaw", "inputPitch", "cause");
        });
    }

    public PaperImperatBuilder setAdventureProvider(AdventureProvider<CommandSender> adventureProvider) {
        this.adventureProvider = adventureProvider;
        return this;
    }


    public PaperImperatBuilder setOverrideBrigadierMessaging(boolean enabled) {
        this.overrideBrigadierMessaging = enabled;
        return this;
    }

    @Override
    public @NotNull PaperImperat build() {
        if (this.adventureProvider == null) {
            this.adventureProvider = this.loadAdventure();
        }
        return new PaperImperat(plugin, adventureProvider, this.config, overrideBrigadierMessaging);
    }

    @SuppressWarnings("ConstantConditions")
    private @NotNull AdventureProvider<CommandSender> loadAdventure() {
        if (Reflections.findClass("net.kyori.adventure.audience.Audience")) {
            if (Audience.class.isAssignableFrom(CommandSender.class)) {
                return new CastingAdventure<>() {
                };
            }
        }
        return new EmptyAdventure<>();
    }
}
