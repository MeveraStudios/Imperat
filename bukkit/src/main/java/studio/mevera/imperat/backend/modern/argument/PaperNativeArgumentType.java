package studio.mevera.imperat.backend.modern.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.providers.SuggestionProvider;

/**
 * Common contract for Imperat-side
 * {@link studio.mevera.imperat.command.arguments.type.ArgumentType ArgumentType}
 * implementations that expose a Paper Brigadier-native counterpart for
 * client rendering + native autocomplete on the modern Paper backend.
 *
 * <p>Two cases collapse into this single SPI:
 * <ul>
 *   <li><b>Native-driven types</b> ({@link PaperBukkitArgumentType}) — server-side
 *       parse runs the native parser then maps with a resolver. The same
 *       native is sent to the client. Used for {@code World}, {@code GameMode},
 *       {@code ItemStack}, etc.</li>
 *   <li><b>Imperat-side parsers with native rendering</b> (e.g.
 *       {@code PaperTargetSelectorArgument}) — custom server-side parse
 *       logic stays in the Imperat parser; the native is exposed only
 *       so the client gets correct coloring + autocomplete (selector
 *       charset, filter keys, etc.).</li>
 * </ul></p>
 *
 * <p>{@link studio.mevera.imperat.backend.modern.ModernPaperBrigadierManager}
 * uses a single {@code instanceof PaperNativeArgumentType} check across
 * positional-arg registration, flag-value registration, suggestion
 * routing, and flag-value suggestion routing — no per-case branching.</p>
 *
 * @since 4.0.0
 */
public interface PaperNativeArgumentType {

    /**
     * Paper Brigadier {@link ArgumentType} sent to the client for syntax
     * validation, coloring, and native autocomplete. Server-side parsing
     * is owned by the implementing
     * {@link studio.mevera.imperat.command.arguments.type.ArgumentType ArgumentType}'s
     * own {@code parse} method.
     */
    @NotNull ArgumentType<?> nativeType();

    /**
     * Imperat-side suggestion provider that bridges into the native type's
     * {@link ArgumentType#listSuggestions} pipeline. Implementations can
     * return this from
     * {@link studio.mevera.imperat.command.arguments.type.ArgumentType#getSuggestionProvider()}
     * so the Imperat tree suggester emits the SAME completions the modern
     * Paper backend would emit through Brigadier — keeps the plain
     * fallback / async-tab paths consistent with native rendering.
     *
     * <p>Default implementation routes through
     * {@link PaperBrigadierSuggestionBridge#bridge}; override only if a
     * non-Brigadier suggestion source is needed.</p>
     */
    default @NotNull SuggestionProvider<BukkitCommandSource> bridgedSuggestionProvider() {
        return (suggestionContext, argument) ->
                       PaperBrigadierSuggestionBridge.bridge(suggestionContext, nativeType());
    }
}
