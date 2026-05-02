package studio.mevera.imperat.backend.modern.type;

import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.backend.modern.argument.PaperNativeArgumentType;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.type.TargetSelectorArgument;

/**
 * Modern-Paper-aware variant of {@link TargetSelectorArgument}. Inherits
 * the Imperat-side selector parser (server-side filter logic, custom
 * {@code [...]} predicate fields) and additionally exposes Paper's
 * {@code ArgumentTypes.entities()} as the Brigadier-native type, so the
 * Mojang client renders {@code @e[type=zombie,name=Bob]} green and
 * provides native selector autocomplete (selector char menu, filter
 * keys, type cycling).
 *
 * @since 4.0.0 (Paper module)
 */
public final class PaperTargetSelectorArgument extends TargetSelectorArgument<BukkitCommandSource> implements PaperNativeArgumentType {

    @Override
    public @NotNull ArgumentType<?> nativeType() {
        return ArgumentTypes.entities();
    }

    /**
     * Override Imperat's default suggestion provider so the tree suggester
     * (plain CommandMap fallback, async-tab path) emits the SAME
     * selector-aware completions the modern Paper backend serves through
     * Brigadier. Without this the parent {@link TargetSelectorArgument}
     * would emit its own selector char menu only — bridge to Mojang's
     * native pipeline for richer autocomplete (filter keys, type cycling,
     * etc.).
     */
    @Override
    public SuggestionProvider<BukkitCommandSource> getSuggestionProvider() {
        return bridgedSuggestionProvider();
    }
}
