package studio.mevera.imperat.backend.modern.argument;

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

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Factory mirror of Paper's
 * {@link io.papermc.paper.command.brigadier.argument.ArgumentTypes}. Each
 * method returns a {@link PaperArgumentType} pairing the native Paper
 * Brigadier {@code ArgumentType} with a resolver mapping its parsed form
 * to a friendly Java type.
 *
 * <p>Selector / position resolvers receive the executing
 * {@link io.papermc.paper.command.brigadier.CommandSourceStack} at parse
 * time via {@link PaperBukkitArgumentType} — registration is stack-free.</p>
 *
 * @since 4.0.0 (Paper module)
 */
public final class PaperArgumentTypes {

    private PaperArgumentTypes() {
    }

    // ===== Entity / Player selectors =====================================

    /** Single-entity selector — resolved at parse time against the source. */
    public static PaperArgumentType<EntitySelectorArgumentResolver, Entity> entity() {
        return PaperArgumentType.of(ArgumentTypes.entity(), (resolver, stack) -> {
            try {
                List<Entity> resolved = resolver.resolve(stack);
                return resolved.isEmpty() ? null : resolved.get(0);
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    /** Multi-entity selector — resolved list at parse time. */
    public static PaperArgumentType<EntitySelectorArgumentResolver, List<Entity>> entities() {
        return PaperArgumentType.of(ArgumentTypes.entities(), (resolver, stack) -> {
            try {
                return resolver.resolve(stack);
            } catch (Throwable ex) {
                return List.of();
            }
        });
    }

    /**
     * {@link studio.mevera.imperat.selector.TargetSelector} — wraps Paper's
     * native multi-entity selector resolver so user methods can declare a
     * {@code TargetSelector} parameter and receive the same iterable
     * abstraction as the legacy bukkit {@link studio.mevera.imperat.type.TargetSelectorArgument}
     * exposes, while still benefiting from Mojang client-native selector
     * autocomplete (cycling through {@code @p}, {@code @a}, etc.).
     */
    public static PaperArgumentType<EntitySelectorArgumentResolver, studio.mevera.imperat.selector.TargetSelector> targetSelector() {
        return PaperArgumentType.of(ArgumentTypes.entities(), (resolver, stack) -> {
            try {
                List<Entity> resolved = resolver.resolve(stack);
                return studio.mevera.imperat.selector.TargetSelector.of(
                        studio.mevera.imperat.selector.SelectionType.UNKNOWN, resolved);
            } catch (Throwable ex) {
                return studio.mevera.imperat.selector.TargetSelector.empty();
            }
        });

    }

    /** Single-player selector — picks first match. */
    public static PaperArgumentType<PlayerSelectorArgumentResolver, Player> player() {
        return PaperArgumentType.of(ArgumentTypes.player(), (resolver, stack) -> {
            try {
                List<Player> resolved = resolver.resolve(stack);
                return resolved.isEmpty() ? null : resolved.get(0);
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    /** Multi-player selector. */
    public static PaperArgumentType<PlayerSelectorArgumentResolver, List<Player>> players() {
        return PaperArgumentType.of(ArgumentTypes.players(), (resolver, stack) -> {
            try {
                return resolver.resolve(stack);
            } catch (Throwable ex) {
                return List.of();
            }
        });
    }

    /** Player-profile list (offline-aware). */
    public static PaperArgumentType<PlayerProfileListResolver, Collection<com.destroystokyo.paper.profile.PlayerProfile>>
    playerProfiles() {
        return PaperArgumentType.of(ArgumentTypes.playerProfiles(), (resolver, stack) -> {
            try {
                return resolver.resolve(stack);
            } catch (Throwable ex) {
                return List.of();
            }
        });
    }

    // ===== Positions =====================================================

    public static PaperArgumentType<BlockPositionResolver, io.papermc.paper.math.BlockPosition> blockPosition() {
        return PaperArgumentType.of(ArgumentTypes.blockPosition(), (resolver, stack) -> {
            try {
                return resolver.resolve(stack);
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    public static PaperArgumentType<FinePositionResolver, io.papermc.paper.math.FinePosition> finePosition() {
        return finePosition(false);
    }

    public static PaperArgumentType<FinePositionResolver, io.papermc.paper.math.FinePosition>
    finePosition(boolean centerIntegers) {
        return PaperArgumentType.of(ArgumentTypes.finePosition(centerIntegers), (resolver, stack) -> {
            try {
                return resolver.resolve(stack);
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    public static PaperArgumentType<RotationResolver, io.papermc.paper.math.Rotation> rotation() {
        return PaperArgumentType.of(ArgumentTypes.rotation(), (resolver, stack) -> {
            try {
                return resolver.resolve(stack);
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

    public static PaperArgumentType<Integer, Integer> time(int mintime) {
        return PaperArgumentType.identity(ArgumentTypes.time(mintime));
    }

    // ===== Misc =====================================================

    public static PaperArgumentType<UUID, UUID> uuid() {
        return PaperArgumentType.identity(ArgumentTypes.uuid());
    }

    public static <T> PaperArgumentType<T, T> resource(@NotNull RegistryKey<T> registryKey) {
        return PaperArgumentType.identity(ArgumentTypes.resource(registryKey));
    }

    public static <T> PaperArgumentType<TypedKey<T>, TypedKey<T>> resourceKey(@NotNull RegistryKey<T> registryKey) {
        return PaperArgumentType.identity(ArgumentTypes.resourceKey(registryKey));
    }
}
