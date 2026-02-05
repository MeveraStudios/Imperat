package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeCapturer;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for defining parameter types in a command processing framework.
 * This class handles the basic functionality for managing type information and
 * suggestions for parameters.
 *
 * @param <S> The type of the source from which the command originates.
 * @param <T> The type of the parameter being handled.
 */
public abstract class ArgumentType<S extends Source, T>
        extends TypeCapturer{

    /**
     * Encapsulates type information for the parameter being handled.
     * This instance is used to collect and manage type-related information
     * and operations specific to the parameter type.
     */
    protected final Type type;
    /**
     * A list storing suggestions for parameter types
     * in a command processing framework.
     */
    protected final List<String> suggestions = new ArrayList<>();

    /**
     * Constructs a new BaseArgumentType with an automated {@link TypeWrap}
     */
    public ArgumentType() {
        this.type = this.extractType(ArgumentType.class, 1);
    }

    /**
     * Constructs a new BaseArgumentType with the given TypeWrap.
     *
     * @param type The type of the parameter.
     */
    public ArgumentType(final Class<T> type) {
        this.type = type;
    }

    /**
     * Constructs a new BaseArgumentType with the given TypeWrap.
     *
     * @param type The type of the parameter.
     */
    public ArgumentType(final Type type) {
        this.type = type;
    }

    /**
     * Retrieves the Type associated with this BaseArgumentType.
     *
     * @return the Type associated with this parameter type.
     */
    public Type type() {
        return type;
    }


    /**
     * Resolves the parameter value from the given input string, using the provided
     * execution context and input stream.
     *
     * @param context the execution context.
     * @param cursor the command input stream.
     * @param correspondingInput the raw input corresponding the current cursor.
     * @return the resolved value, or {@code null} if resolution fails.
     * @throws CommandException if resolution fails due to an error.
     */
    public abstract @Nullable T resolve(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput
    ) throws CommandException;

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    public SuggestionResolver<S> getSuggestionResolver() {
        return suggestions.isEmpty() ? null : SuggestionResolver.staticSuggestions(suggestions);
    }

    /**
     * Determines whether the provided input matches the expected format or criteria
     * for a given command parameter. this is used during {@link CommandTree#contextMatch(Context, ArgumentInput)}
     *
     * @param rawPosition The raw position of the argument in the input.
     * @param context The context to be matched, providing necessary information about the command execution environment.
     * @param parameter The command parameter that provides context for the input handling.
     * @return true if the input matches the expected criteria; false otherwise.
     */
    public boolean matchesInput(int rawPosition, Context<S> context, Argument<S> parameter) {
        return true;
    }

    /**
     * Returns the default value supplier for the given source and command parameter.
     * By default, this returns an empty supplier, indicating no default value.
     *
     * @return an {@link OptionalValueSupplier} providing the default value, or empty if none.
     */
    @ApiStatus.AvailableSince("1.9.1")
    public OptionalValueSupplier supplyDefaultValue() {
        return OptionalValueSupplier.empty();
    }

    /**
     * Checks if the given type is related to this parameter type.
     *
     * @param type the type to check.
     * @return {@code true} if the types are related, {@code false} otherwise.
     */
    public boolean isRelatedToType(Type type) {
        return TypeUtility.areRelatedTypes(type, this.type());
    }

    /**
     * Checks if the given type exactly matches this parameter type.
     *
     * @param type the type to check.
     * @return {@code true} if the types match exactly, {@code false} otherwise.
     */
    public boolean equalsExactly(Type type) {
        return TypeUtility.matches(type, type());
    }


    /**
     * Gets a {@link TypeWrap} for the handled type.
     *
     * @return the wrapped type.
     */
    @SuppressWarnings("unchecked")
    public TypeWrap<T> wrappedType() {
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
    public boolean isGreedy(Argument<S> parameter) {
        return false;
    }

    /**
     * Returns the number of arguments consumed by this parameter type.
     * By default, returns {@code 1}.
     *
     * @return the number of consumed arguments.
     */
    @ApiStatus.AvailableSince("2.1.0")
    public int getNumberOfParametersToConsume() {
        return 1;
    }

    /**
     * Adds the provided suggestions to the current list of suggestions for this parameter type.
     *
     * @param suggestions The array of suggestions to be added.
     */
    public void addStaticSuggestions(String... suggestions) {
        this.suggestions.addAll(List.of(suggestions));
    }

    @ApiStatus.AvailableSince("3.0.0")
    public Priority priority() {
        return Priority.NORMAL;
    }
}
