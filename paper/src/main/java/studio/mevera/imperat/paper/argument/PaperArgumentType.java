package studio.mevera.imperat.paper.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * SPI describing a Paper Brigadier argument type bound to a friendly
 * resolver. Provides:
 * <ul>
 *   <li>The native Paper {@link ArgumentType} to register with Brigadier
 *       (so the client gets native suggestions / validation).</li>
 *   <li>A resolver that converts Brigadier's native parsed form
 *       (e.g. {@code EntitySelectorArgumentResolver}) into the friendly
 *       Java type the user's command method expects (e.g. {@code Player}).</li>
 * </ul>
 *
 * <p>One {@code PaperArgumentType} per Java type the framework wants to
 * accept — instances are produced by {@link PaperArgumentTypes}.</p>
 *
 * @param <N> the Brigadier-native parsed form (resolver / value)
 * @param <T> the friendly Java type exposed to user command methods
 *
 * @since 4.0.0 (Paper module)
 */
public interface PaperArgumentType<N, T> {

    /**
     * Constructs an instance from a native Paper Brigadier
     * {@link ArgumentType} and a function that maps its parsed result
     * into the friendly Java type. Used by {@link PaperArgumentTypes}.
     */
    static <N, T> @NotNull PaperArgumentType<N, T> of(
            @NotNull ArgumentType<N> nativeType,
            @NotNull Function<N, T> resolver
    ) {
        return new PaperArgumentTypeImpl<>(nativeType, resolver);
    }

    /**
     * Identity-resolver variant for argument types whose Brigadier-native
     * form is already the friendly type (e.g. {@code World}, {@code GameMode},
     * {@code NamespacedKey}, {@code ItemStack}).
     */
    static <T> @NotNull PaperArgumentType<T, T> identity(@NotNull ArgumentType<T> nativeType) {
        return new PaperArgumentTypeImpl<>(nativeType, t -> t);
    }

    @NotNull ArgumentType<N> nativeType();

    @NotNull Function<N, T> resolver();
}
