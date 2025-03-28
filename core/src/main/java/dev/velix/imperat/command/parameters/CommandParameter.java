package dev.velix.imperat.command.parameters;

import dev.velix.imperat.annotations.parameters.AnnotatedParameter;
import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.Description;
import dev.velix.imperat.command.DescriptionHolder;
import dev.velix.imperat.command.PermissionHolder;
import dev.velix.imperat.command.parameters.type.ParameterType;
import dev.velix.imperat.command.parameters.type.ParameterTypes;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.resolvers.SuggestionResolver;
import dev.velix.imperat.supplier.OptionalValueSupplier;
import dev.velix.imperat.util.Preconditions;
import dev.velix.imperat.util.TypeWrap;
import org.jetbrains.annotations.*;

import java.lang.reflect.Type;

/**
 * Represents the command parameter required
 * by the usage of the command itself
 */
@ApiStatus.AvailableSince("1.0.0")
public interface CommandParameter<S extends Source> extends PermissionHolder, DescriptionHolder {

    static <S extends Source, T> CommandParameter<S> of(
        String name,
        ParameterType<S, T> type,
        @Nullable String permission,
        Description description,
        boolean optional,
        boolean greedy,
        @NotNull OptionalValueSupplier<T> valueSupplier,
        @Nullable SuggestionResolver<S> suggestionResolver
    ) {
        Preconditions.notNull(name, "name");
        Preconditions.notNull(type, "type");
        Preconditions.checkArgument(!type.equalsExactly(Object.class), "Type cannot be `Object`");

        return new NormalCommandParameter<>(
            name, type, permission, description, optional,
            greedy, valueSupplier, suggestionResolver
        );
    }

    static <S extends Source, T> ParameterBuilder<S, T> required(String name, ParameterType<S, T> type) {
        return new ParameterBuilder<>(name, type, false);
    }

    static <S extends Source> ParameterBuilder<S, Integer> requiredInt(String name) {
        return required(name, ParameterTypes.numeric(Integer.class));
    }

    static <S extends Source> ParameterBuilder<S, Long> requiredLong(String name) {
        return required(name, ParameterTypes.numeric(Long.class));
    }

    static <S extends Source> ParameterBuilder<S, Double> requiredDouble(String name) {
        return required(name, ParameterTypes.numeric(Double.class));
    }

    static <S extends Source> ParameterBuilder<S, Float> requiredFloat(String name) {
        return required(name, ParameterTypes.numeric(Float.class));
    }

    static <S extends Source> ParameterBuilder<S, Boolean> requiredBoolean(String name) {
        return required(name, ParameterTypes.bool());
    }

    static <S extends Source> ParameterBuilder<S, String> requiredText(String name) {
        return required(name, ParameterTypes.string());
    }

    //TODO REPLACE GREEDY SYSTEM WITH PARAMETER TYPE SYSTEM
    static <S extends Source> ParameterBuilder<S, String> requiredGreedy(String name) {
        return new ParameterBuilder<>(name, ParameterTypes.string(), false, true);
    }

    static <S extends Source, T> ParameterBuilder<S, T> optional(String name, ParameterType<S, T> token) {
        return new ParameterBuilder<>(name, token, true);
    }


    static <S extends Source> ParameterBuilder<S, Integer> optionalInt(String name) {
        return optional(name, ParameterTypes.numeric(Integer.class));
    }

    static <S extends Source> ParameterBuilder<S, Long> optionalLong(String name) {
        return optional(name, ParameterTypes.numeric(Long.class));
    }

    static <S extends Source> ParameterBuilder<S, Double> optionalDouble(String name) {
        return optional(name, ParameterTypes.numeric(Double.class));
    }

    static <S extends Source> ParameterBuilder<S, Float> optionalFloat(String name) {
        return optional(name, ParameterTypes.numeric(Float.class));
    }

    static <S extends Source> ParameterBuilder<S, Boolean> optionalBoolean(String name) {
        return optional(name, ParameterTypes.bool());
    }

    static <S extends Source> ParameterBuilder<S, String> optionalText(String name) {
        return optional(name, ParameterTypes.string());
    }

    //TODO REPLACE GREEDY SYSTEM WITH PARAMETER TYPE SYSTEM
    static <S extends Source> ParameterBuilder<S, String> optionalGreedy(String name) {
        return new ParameterBuilder<>(name, ParameterTypes.string(), true, true);
    }

    static <S extends Source, T> FlagBuilder<S, T> flag(
        String name,
        ParameterType<S, T> inputType
    ) {
        return FlagBuilder.ofFlag(name, inputType);
    }

    static <S extends Source> FlagBuilder<S, Boolean> flagSwitch(String name) {
        return FlagBuilder.ofSwitch(name);
    }

    /**
     * @return the name of the parameter
     */
    String name();

    /**
     * @return the parent of this parameter
     */
    @Nullable
    Command<S> parent();

    /**
     * Sets parent command for a parameter
     *
     * @param parentCommand the parameter's owning command
     */
    void parent(Command<S> parentCommand);

    /**
     * @return the index of this parameter
     */
    int position();

    /**
     * Sets the position of this parameter in a syntax
     * DO NOT USE THIS FOR ANY REASON unless it's necessary to do so
     *
     * @param position the position to set
     */
    @ApiStatus.Internal
    void position(int position);

    /**
     * @return the value valueType-token of this parameter
     */
    TypeWrap<?> wrappedType();

    /**
     * @return the value valueType of this parameter
     */
    default Type valueType() {
        return wrappedType().getType();
    }

    /**
     * Retrieves the parameter type associated with this command parameter.
     *
     * @return the {@link ParameterType} of the command parameter
     */
    @NotNull ParameterType<S, ?> type();

    /**
     * @return the default value if it's input is not present
     * in case of the parameter being optional
     */
    @NotNull
    <T> OptionalValueSupplier<T> getDefaultValueSupplier();

    /**
     * @return whether this is an optional argument
     */
    boolean isOptional();

    /**
     * @return checks whether this parameter is a flag
     */
    boolean isFlag();

    /**
     * Casts the parameter to a flag parameter
     *
     * @return the parameter as a flag
     */
    FlagParameter<S> asFlagParameter();

    /**
     * @return checks whether this parameter
     * consumes all the args input after it.
     */
    boolean isGreedy();

    boolean isGreedyString();

    /**
     * @return checks whether this usage param is a command name
     */
    default boolean isCommand() {
        return this instanceof Command;
    }

    /**
     * Casts the parameter to a subcommand/command
     *
     * @return the parameter as a command
     */
    Command<S> asCommand();

    /**
     * @return Whether this usage parameter has been constructed
     * using the annotations through methods or not
     */
    default boolean isAnnotated() {
        return this instanceof AnnotatedParameter;
    }

    /**
     * Casts the parameter to a parameter with annotations
     *
     * @return the parameter as annotated one
     * @see AnnotatedParameter
     */
    default AnnotatedParameter<S> asAnnotated() {
        return (AnnotatedParameter<S>) this;
    }

    /**
     * Fetches the suggestion resolver linked to this
     * command parameter.
     *
     * @return the {@link SuggestionResolver} for a resolving suggestion
     */
    @Nullable
    SuggestionResolver<S> getSuggestionResolver();

    /**
     * Formats the usage parameter*
     *
     * @return the formatted parameter
     */
    String format();

    default boolean isNumeric() {
        return this instanceof NumericParameter;
    }

    default NumericParameter<S> asNumeric() {
        return (NumericParameter<S>) this;
    }

    /**
     * Checks if this parameter has same name and valueType to the other {@link CommandParameter}
     * unlike `CommandParameter#equals(Object)`,
     * if both parameters are only different in their parent {@link Command},
     * it would still return true
     *
     * @param parameter the parameter to compare to
     * @return Whether this parameter has same name and valueType to the other {@link CommandParameter} or not
     */
    boolean similarTo(CommandParameter<?> parameter);

    default boolean isRequired() {
        return !isOptional();
    }
}
