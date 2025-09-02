package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

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
    @Nullable T resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> inputStream, @NotNull String input) throws ImperatException;

    /**
     * Gets the suggestion resolver for this parameter type.
     *
     * @return the suggestion resolver.
     */
    SuggestionResolver<S> getSuggestionResolver();

    /**
     * Checks if the given input string matches this parameter type for the specified parameter.
     *
     * @param rawPosition the raw position of the argument in the input.
     * @param context the context to be matched.
     * @param parameter the command parameter.
     * @return {@code true} if the input matches, {@code false} otherwise.
     */
    boolean matchesInput(int rawPosition, Context<S> context, CommandParameter<S> parameter);

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
    
    /**
     * Determines if this parameter type is greedy, meaning it consumes multiple arguments.
     * By default, returns {@code false}.
     *
     * @param parameter the command parameter.
     * @return {@code true} if greedy, {@code false} otherwise.
     */
    @ApiStatus.AvailableSince("2.1.0")
    default boolean isGreedy(CommandParameter<S> parameter) {
        return false;
    }
    
    /**
     * Returns the number of arguments consumed by this parameter type.
     * By default, returns {@code 1}.
     *
     * @return the number of consumed arguments.
     */
    @ApiStatus.AvailableSince("2.1.0")
    default int getConsumedArguments() {
        return 1;
    }
    
}
