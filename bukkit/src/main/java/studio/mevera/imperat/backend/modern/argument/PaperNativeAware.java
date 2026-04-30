package studio.mevera.imperat.backend.modern.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;

/**
 * Marker SPI for Imperat-side
 * {@link studio.mevera.imperat.command.arguments.type.ArgumentType
 * ArgumentType} implementations that want Paper-native Brigadier rendering
 * + client-side suggestions on the modern Paper backend.
 *
 * <p>The {@link studio.mevera.imperat.BaseBrigadierManager} default flow
 * uses {@code StringArgumentType} for any non-{@link PaperBukkitArgumentType}
 * — that means stock unquoted-word charset, halts at {@code [}, {@code =},
 * {@code @}, etc., which paints valid native syntax (entity selectors,
 * block states, NBT tags) red on the client.</p>
 *
 * <p>An Imperat-side parser can keep its custom server-side parse logic
 * AND opt into Mojang client-native rendering by implementing this
 * interface and returning the matching Paper {@code ArgumentType}.
 * {@link studio.mevera.imperat.backend.modern.ModernPaperBrigadierManager}
 * consults the bridge for both Brigadier registration ({@code getArgumentType})
 * and suggestion routing ({@code createSuggestionProvider}), so client
 * coloring + native autocomplete (selector char menu, filter-key cycling,
 * etc.) come for free without sacrificing the server-side parser.</p>
 *
 * @since 4.0.0
 */
public interface PaperNativeAware {

    /**
     * Paper Brigadier {@link ArgumentType} sent to the client for syntax
     * validation, coloring, and native autocomplete. Server-side parsing
     * still goes through the implementing Imperat-side {@code ArgumentType}.
     */
    @NotNull ArgumentType<?> paperNativeType();
}
