package studio.mevera.imperat.paper.argument;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.predicate.ItemStackPredicate;
import io.papermc.paper.command.brigadier.argument.range.DoubleRangeProvider;
import io.papermc.paper.command.brigadier.argument.range.IntegerRangeProvider;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.RotationResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Factory mirror of Paper's
 * {@link io.papermc.paper.command.brigadier.argument.ArgumentTypes}. Each
 * method returns a {@link PaperArgumentType} that pairs the native Paper
 * Brigadier {@code ArgumentType} with a resolver mapping its parsed form
 * to a friendly Java type that command methods can consume directly.
 *
 * <p>Resolvers handle the {@code /execute}-context safely by fetching the
 * effective {@link CommandSourceStack} where required (entity/player
 * selectors, position resolvers, etc.). Where Paper's native form is
 * already the friendly type ({@code World}, {@code GameMode},
 * {@code ItemStack}, {@code NamespacedKey}, etc.) the resolver is the
 * identity function.</p>
 *
 * <p>Several factories require a current Brigadier {@link CommandContext}
 * to resolve their value (selectors, positions). Those expose the
 * resolver function as a 1-arg lambda accepting the resolver type itself
 * — the wrapper {@link PaperBukkitArgumentType} provides the context at
 * parse time.</p>
 *
 * @since 4.0.0 (Paper module)
 */
public final class PaperArgumentTypes {

    private PaperArgumentTypes() {
    }

    // ===== Entity / Player selectors =====================================

    /**
     * Single-entity selector — friendly result is the resolved {@link Entity}
     * (resolver picks the first matching entity, or {@code null} if none).
     */
    public static PaperArgumentType<EntitySelectorArgumentResolver, Entity> entity(@NotNull CommandContext<CommandSourceStack> ctx) {
        return PaperArgumentType.of(ArgumentTypes.entity(), resolver -> {
            try {
                List<Entity> resolved = resolver.resolve(ctx.getSource());
                return resolved.isEmpty() ? null : resolved.get(0);
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    /** Multi-entity selector — friendly result is the resolved list of entities. */
    public static PaperArgumentType<EntitySelectorArgumentResolver, List<Entity>> entities(@NotNull CommandContext<CommandSourceStack> ctx) {
        return PaperArgumentType.of(ArgumentTypes.entities(), resolver -> {
            try {
                return resolver.resolve(ctx.getSource());
            } catch (Throwable ex) {
                return List.of();
            }
        });
    }

    /** Single-player selector. */
    public static PaperArgumentType<PlayerSelectorArgumentResolver, Player> player(@NotNull CommandContext<CommandSourceStack> ctx) {
        return PaperArgumentType.of(ArgumentTypes.player(), resolver -> {
            try {
                List<Player> resolved = resolver.resolve(ctx.getSource());
                return resolved.isEmpty() ? null : resolved.get(0);
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    /** Multi-player selector. */
    public static PaperArgumentType<PlayerSelectorArgumentResolver, List<Player>> players(@NotNull CommandContext<CommandSourceStack> ctx) {
        return PaperArgumentType.of(ArgumentTypes.players(), resolver -> {
            try {
                return resolver.resolve(ctx.getSource());
            } catch (Throwable ex) {
                return List.of();
            }
        });
    }

    /**
     * Player-profile list (offline-aware). Friendly result is the
     * collection of player profiles resolved against the command source.
     */
    public static PaperArgumentType<PlayerProfileListResolver, Collection<com.destroystokyo.paper.profile.PlayerProfile>>
    playerProfiles(@NotNull CommandContext<CommandSourceStack> ctx) {
        return PaperArgumentType.of(ArgumentTypes.playerProfiles(), resolver -> {
            try {
                return resolver.resolve(ctx.getSource());
            } catch (Throwable ex) {
                return List.of();
            }
        });
    }

    // ===== Positions =====================================================

    /**
     * Block position — friendly result is the resolved
     * {@link io.papermc.paper.math.BlockPosition} (Paper's vec3i analogue).
     */
    public static PaperArgumentType<BlockPositionResolver, io.papermc.paper.math.BlockPosition>
    blockPosition(@NotNull CommandContext<CommandSourceStack> ctx) {
        return PaperArgumentType.of(ArgumentTypes.blockPosition(), resolver -> {
            try {
                return resolver.resolve(ctx.getSource());
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    /**
     * Fine position — sub-block precision. {@code centerIntegers=false}
     * by default; pass {@code true} to centre whole numbers (+0.5).
     */
    public static PaperArgumentType<FinePositionResolver, io.papermc.paper.math.FinePosition>
    finePosition(@NotNull CommandContext<CommandSourceStack> ctx) {
        return finePosition(ctx, false);
    }

    public static PaperArgumentType<FinePositionResolver, io.papermc.paper.math.FinePosition>
    finePosition(@NotNull CommandContext<CommandSourceStack> ctx, boolean centerIntegers) {
        return PaperArgumentType.of(ArgumentTypes.finePosition(centerIntegers), resolver -> {
            try {
                return resolver.resolve(ctx.getSource());
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    /** Rotation argument (yaw, pitch). */
    public static PaperArgumentType<RotationResolver, io.papermc.paper.math.Rotation>
    rotation(@NotNull CommandContext<CommandSourceStack> ctx) {
        return PaperArgumentType.of(ArgumentTypes.rotation(), resolver -> {
            try {
                return resolver.resolve(ctx.getSource());
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    // ===== World / Game state ==========================================

    public static PaperArgumentType<World, World> world() {
        return PaperArgumentType.identity(ArgumentTypes.world());
    }

    public static PaperArgumentType<GameMode, GameMode> gameMode() {
        return PaperArgumentType.identity(ArgumentTypes.gameMode());
    }

    public static PaperArgumentType<ItemStack, ItemStack> itemStack() {
        return PaperArgumentType.identity(ArgumentTypes.itemStack());
    }

    public static PaperArgumentType<ItemStackPredicate, ItemStackPredicate> itemPredicate() {
        return PaperArgumentType.identity(ArgumentTypes.itemPredicate());
    }

    public static PaperArgumentType<BlockData, BlockData> blockState() {
        // Paper's #blockState() returns a BlockState argument; we expose
        // BlockData since most user code wants the DSL-friendly form.
        @SuppressWarnings("unchecked")
        com.mojang.brigadier.arguments.ArgumentType<BlockData> nativeType =
                (com.mojang.brigadier.arguments.ArgumentType<BlockData>) (com.mojang.brigadier.arguments.ArgumentType<?>) ArgumentTypes.blockState();
        return PaperArgumentType.identity(nativeType);
    }

    // ===== Text / colour ================================================

    public static PaperArgumentType<NamedTextColor, NamedTextColor> namedColor() {
        return PaperArgumentType.identity(ArgumentTypes.namedColor());
    }

    public static PaperArgumentType<Component, Component> component() {
        return PaperArgumentType.identity(ArgumentTypes.component());
    }

    public static PaperArgumentType<Style, Style> style() {
        return PaperArgumentType.identity(ArgumentTypes.style());
    }

    // ===== Scoreboard =================================================

    public static PaperArgumentType<DisplaySlot, DisplaySlot> scoreboardDisplaySlot() {
        return PaperArgumentType.identity(ArgumentTypes.scoreboardDisplaySlot());
    }

    public static PaperArgumentType<Criteria, Criteria> objectiveCriteria() {
        return PaperArgumentType.identity(ArgumentTypes.objectiveCriteria());
    }

    // ===== Keys =======================================================

    public static PaperArgumentType<NamespacedKey, NamespacedKey> namespacedKey() {
        return PaperArgumentType.identity(ArgumentTypes.namespacedKey());
    }

    public static PaperArgumentType<Key, Key> key() {
        return PaperArgumentType.identity(ArgumentTypes.key());
    }

    // ===== Numerics / time ============================================

    public static PaperArgumentType<IntegerRangeProvider, IntegerRangeProvider> integerRange() {
        return PaperArgumentType.identity(ArgumentTypes.integerRange());
    }

    public static PaperArgumentType<DoubleRangeProvider, DoubleRangeProvider> doubleRange() {
        return PaperArgumentType.identity(ArgumentTypes.doubleRange());
    }

    public static PaperArgumentType<Integer, Integer> time() {
        return time(0);
    }

    public static PaperArgumentType<Integer, Integer> time(final int mintime) {
        return PaperArgumentType.identity(ArgumentTypes.time(mintime));
    }

    // ===== Misc =====================================================

    public static PaperArgumentType<UUID, UUID> uuid() {
        return PaperArgumentType.identity(ArgumentTypes.uuid());
    }

    /**
     * A generic Bukkit registry resource lookup. Friendly result is the
     * resolved registry value (e.g. {@code EntityType}, {@code Material},
     * {@code Sound}).
     *
     * @param registryKey the registry to query
     * @param <T> registry value type
     */
    public static <T> PaperArgumentType<T, T> resource(@NotNull RegistryKey<T> registryKey) {
        return PaperArgumentType.identity(ArgumentTypes.resource(registryKey));
    }

    /**
     * Typed key for a Bukkit registry — friendly result is a
     * {@link TypedKey} pointing at the registry entry without forcing
     * resolution.
     */
    public static <T> PaperArgumentType<TypedKey<T>, TypedKey<T>> resourceKey(@NotNull RegistryKey<T> registryKey) {
        return PaperArgumentType.identity(ArgumentTypes.resourceKey(registryKey));
    }

    // ===== Convenience: standalone (no-context) versions ================
    // Some types don't actually need the CommandContext to resolve — these
    // are exposed as plain factory methods for use in registry-default
    // wiring where the context isn't yet available.

    /** Single-player selector pre-bound to fetch the FIRST player against a context. */
    public static PaperArgumentType<PlayerSelectorArgumentResolver, PlayerSelectorArgumentResolver> playerResolver() {
        return PaperArgumentType.identity(ArgumentTypes.player());
    }

    /** Single-entity selector pre-bound (defer resolution to caller). */
    public static PaperArgumentType<EntitySelectorArgumentResolver, EntitySelectorArgumentResolver> entityResolver() {
        return PaperArgumentType.identity(ArgumentTypes.entity());
    }

    /** Resolves a snowflake {@link UUID} from the active context — convenience. */
    public static @Nullable UUID resolveUuid(@NotNull String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Server context handle — used by callers needing the live Bukkit {@code Server}. */
    public static @NotNull org.bukkit.Server server() {
        return Bukkit.getServer();
    }
}
