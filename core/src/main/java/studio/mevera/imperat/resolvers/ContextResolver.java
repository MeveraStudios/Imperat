package studio.mevera.imperat.resolvers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;

import java.util.function.Supplier;

/**
 * Resolves a default non-overridable value for
 * missing required arguments
 *
 * @param <T> the valueType of resolver's parameter
 */
@FunctionalInterface
public interface ContextResolver<S extends Source, T> {


    static <S extends Source, T> ContextResolver<S, T> of(T value) {
        return (c, p) -> value;
    }

    static <S extends Source, T> ContextResolver<S, T> of(Supplier<T> supplier) {
        return of(supplier.get());
    }

    /**
     * Resolves a parameter's default value
     * if it has been not input by the user
     *
     * @param context   the context
     * @param parameter the parameter (null if used the classic way)
     * @throws CommandException if resolution fails
     * @return the resolved default-value
     */
    @Nullable
    T resolve(
            @NotNull ExecutionContext<S> context,
            @Nullable ParameterElement parameter
    ) throws CommandException;

}
