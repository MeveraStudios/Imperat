package dev.velix.imperat.command.parameters.type;

import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.OptionalValueSupplier;
import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.resolvers.SuggestionResolver;
import dev.velix.imperat.util.TypeUtility;
import dev.velix.imperat.util.TypeWrap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Represents a type handler for command parameters, providing methods for
 * type resolution, input matching, and suggestions.
 *
 * @param <S> The type of the command source.
 * @param <T> The type of the parameter value.
 */
public interface ParameterType<S extends Source, T> {

    /**
     * Gets the Java {@link Type} this parameter type handles.
     *
     * @return the handled type.
     */
    Type type();

    /**
     * Resolves the parameter value from the given input string, using the provided
     * execution context and input stream.
     *
     * @param context the execution context.
     * @param inputStream the command input stream.
     * @param input the raw input string.
     * @return the resolved value, or {@code null} if resolution fails.
     * @throws ImperatException if resolution fails due to an error.
     */
    @Nullable T resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> inputStream, String input) throws ImperatException;

    /**
     * Gets the suggestion resolver for this parameter type.
     *
     * @return the suggestion resolver.
     */
    SuggestionResolver<S> getSuggestionResolver();

    /**
     * Checks if the given input string matches this parameter type for the specified parameter.
     *
     * @param input the input string.
     * @param parameter the command parameter.
     * @return {@code true} if the input matches, {@code false} otherwise.
     */
    boolean matchesInput(String input, CommandParameter<S> parameter);

    /**
     * Returns the default value supplier for the given source and command parameter.
     * By default, this returns an empty supplier, indicating no default value.
     *
     * @return an {@link OptionalValueSupplier} providing the default value, or empty if none.
     */
    @ApiStatus.AvailableSince("1.9.1")
    default OptionalValueSupplier supplyDefaultValue() {
        return OptionalValueSupplier.empty();
    }

    /**
     * Checks if the given type is related to this parameter type.
     *
     * @param type the type to check.
     * @return {@code true} if the types are related, {@code false} otherwise.
     */
    default boolean isRelatedToType(Type type) {
        return TypeUtility.areRelatedTypes(type, this.type());
    }

    /**
     * Checks if the given type exactly matches this parameter type.
     *
     * @param type the type to check.
     * @return {@code true} if the types match exactly, {@code false} otherwise.
     */
    default boolean equalsExactly(Type type) {
        return TypeUtility.matches(type, type());
    }

    /**
     * Returns a new {@link ParameterType} instance with the specified suggestions.
     *
     * @param suggestions the suggestions to use.
     * @return a new parameter type with suggestions.
     */
    @NotNull ParameterType<S, T> withSuggestions(String... suggestions);

    /**
     * Gets a {@link TypeWrap} for the handled type.
     *
     * @return the wrapped type.
     */
    @SuppressWarnings("unchecked")
    default TypeWrap<T> wrappedType() {
        return (TypeWrap<T>) TypeWrap.of(type());
    }
}
