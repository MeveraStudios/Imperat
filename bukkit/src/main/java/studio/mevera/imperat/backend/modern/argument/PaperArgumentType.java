package studio.mevera.imperat.backend.modern.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * SPI describing a Paper Brigadier argument type bound to a friendly
 * resolver. Provides:
 * <ul>
 *   <li>The native Paper {@link ArgumentType} to register with Brigadier
 *       — Mojang client recognises these IDs and generates suggestions
 *       client-side, so no server-side {@code customSuggestions} dance.</li>
 *   <li>A resolver that converts Brigadier's native parsed form
 *       (e.g. {@code EntitySelectorArgumentResolver}) into the friendly
 *       Java type the user's command method expects (e.g. {@code Player}).
 *       The resolver receives the live {@link CommandSourceStack} so it
 *       can resolve selector-style values against the executing source.</li>
 * </ul>
 *
 * @param <N> the Brigadier-native parsed form (resolver / value)
 * @param <T> the friendly Java type exposed to user command methods
 *
 * @since 4.0.0 (Paper module)
 */
public interface PaperArgumentType<N, T> {

    /**
     * Constructs an instance from a native Paper Brigadier
     * {@link ArgumentType} and a {@link BiFunction} that maps its parsed
     * result + the executing {@link CommandSourceStack} into the friendly
     * Java type. The stack is required for selector resolvers (entity /
     * player / position) — for stack-agnostic types use {@link #of(ArgumentType, java.util.function.Function)}
     * or {@link #identity(ArgumentType)}.
     */
    static <N, T> @NotNull PaperArgumentType<N, T> of(
            @NotNull ArgumentType<N> nativeType,
            @NotNull BiFunction<N, CommandSourceStack, T> resolver
    ) {
        return new PaperArgumentTypeImpl<>(nativeType, resolver);
    }

    /**
     * Constructs an instance from a native Paper Brigadier
     * {@link ArgumentType} and a {@link java.util.function.Function} that
     * maps its parsed result into the friendly Java type — for resolvers
     * that don't depend on the executing source.
     */
    static <N, T> @NotNull PaperArgumentType<N, T> of(
            @NotNull ArgumentType<N> nativeType,
            @NotNull java.util.function.Function<N, T> resolver
    ) {
        return new PaperArgumentTypeImpl<>(nativeType, (parsed, ctx) -> resolver.apply(parsed));
    }

    /**
     * Identity-resolver variant for argument types whose Brigadier-native
     * form is already the friendly type (e.g. {@code World}, {@code GameMode},
     * {@code NamespacedKey}, {@code ItemStack}).
     */
    static <T> @NotNull PaperArgumentType<T, T> identity(@NotNull ArgumentType<T> nativeType) {
        return new PaperArgumentTypeImpl<>(nativeType, (parsed, ctx) -> parsed);
    }

    @NotNull ArgumentType<N> nativeType();

    @NotNull BiFunction<N, CommandSourceStack, T> resolver();
}
