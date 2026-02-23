package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.parameters.AnnotatedArgument;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.DescriptionHolder;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionHolder;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.Preconditions;
import studio.mevera.imperat.util.PriorityList;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the command parameter required
 * by the usage of the command itself
 */
@ApiStatus.AvailableSince("1.0.0")
public interface Argument<S extends Source> extends PermissionHolder, DescriptionHolder {

    static <S extends Source, T> Argument<S> of(
            String name,
            ArgumentType<S, T> type,
            @NotNull PermissionsData permission,
            Description description,
            boolean optional,
            boolean greedy,
            @NotNull DefaultValueProvider valueSupplier,
            @Nullable SuggestionProvider<S> suggestionProvider,
            List<ArgValidator<S>> validators
    ) {
        Preconditions.notNull(name, "name");
        Preconditions.notNull(type, "type");
        Preconditions.checkArgument(!type.equalsExactly(Object.class), "Type cannot be `Object`");

        var param = new NormalArgument<>(
                name, type, permission, description, optional,
                greedy, valueSupplier, suggestionProvider
        );
        for (ArgValidator<S> validator : validators) {
            param.addValidator(validator);
        }
        return param;
    }

    static <S extends Source, T> ArgumentBuilder<S, T> required(String name, ArgumentType<S, T> type) {
        return new ArgumentBuilder<>(name, type, false);
    }

    static <S extends Source> ArgumentBuilder<S, Integer> requiredInt(String name) {
        return required(name, ArgumentTypes.numeric(Integer.class));
    }

    static <S extends Source> ArgumentBuilder<S, Long> requiredLong(String name) {
        return required(name, ArgumentTypes.numeric(Long.class));
    }

    static <S extends Source> ArgumentBuilder<S, Double> requiredDouble(String name) {
        return required(name, ArgumentTypes.numeric(Double.class));
    }

    static <S extends Source> ArgumentBuilder<S, Float> requiredFloat(String name) {
        return required(name, ArgumentTypes.numeric(Float.class));
    }

    static <S extends Source> ArgumentBuilder<S, Boolean> requiredBoolean(String name) {
        return required(name, ArgumentTypes.bool());
    }

    static <S extends Source> ArgumentBuilder<S, String> requiredText(String name) {
        return required(name, ArgumentTypes.string());
    }

    //TODO REPLACE GREEDY SYSTEM WITH PARAMETER TYPE SYSTEM
    static <S extends Source> ArgumentBuilder<S, String> requiredGreedy(String name) {
        return new ArgumentBuilder<>(name, ArgumentTypes.string(), false, true);
    }

    static <S extends Source, T> ArgumentBuilder<S, T> optional(String name, ArgumentType<S, T> token) {
        return new ArgumentBuilder<>(name, token, true);
    }


    static <S extends Source> ArgumentBuilder<S, Integer> optionalInt(String name) {
        return optional(name, ArgumentTypes.numeric(Integer.class));
    }

    static <S extends Source> ArgumentBuilder<S, Long> optionalLong(String name) {
        return optional(name, ArgumentTypes.numeric(Long.class));
    }

    static <S extends Source> ArgumentBuilder<S, Double> optionalDouble(String name) {
        return optional(name, ArgumentTypes.numeric(Double.class));
    }

    static <S extends Source> ArgumentBuilder<S, Float> optionalFloat(String name) {
        return optional(name, ArgumentTypes.numeric(Float.class));
    }

    static <S extends Source> ArgumentBuilder<S, Boolean> optionalBoolean(String name) {
        return optional(name, ArgumentTypes.bool());
    }

    static <S extends Source> ArgumentBuilder<S, String> optionalText(String name) {
        return optional(name, ArgumentTypes.string());
    }

    //TODO REPLACE GREEDY SYSTEM WITH PARAMETER TYPE SYSTEM
    static <S extends Source> ArgumentBuilder<S, String> optionalGreedy(String name) {
        return new ArgumentBuilder<>(name, ArgumentTypes.string(), true, true);
    }

    static <S extends Source, T> FlagBuilder<S, T> flag(
            String name,
            ArgumentType<S, T> inputType
    ) {
        return FlagBuilder.ofFlag(name, inputType);
    }

    static <S extends Source> FlagBuilder<S, Boolean> flagSwitch(String name) {
        return FlagBuilder.ofSwitch(name);
    }

    static <S extends Source> Argument<S> literal(String part) {
        Preconditions.notNull(part, "part");
        Preconditions.checkArgument(!part.isEmpty(), "Literal part cannot be empty");
        Preconditions.checkArgument(part.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_'),
                "Literal part must be alphanumeric or underscore only");

        return of(
                part,
                ArgumentTypes.command(part, new ArrayList<>()),
                PermissionsData.empty(),
                Description.EMPTY,
                false,
                false,
                DefaultValueProvider.empty(),
                null,
                Collections.emptyList()
        );
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
     * @return the {@link ArgumentType} of the command parameter
     */
    @NotNull ArgumentType<S, ?> type();

    /**
     * @return the default value if it's input is not present
     * in case of the parameter being optional
     */
    @NotNull
    DefaultValueProvider getDefaultValueSupplier();

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
    FlagArgument<S> asFlagParameter();

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
        return this instanceof AnnotatedArgument;
    }

    /**
     * Casts the parameter to a parameter with annotations
     *
     * @return the parameter as annotated one
     * @see AnnotatedArgument
     */
    default AnnotatedArgument<S> asAnnotatedArgument() {
        return (AnnotatedArgument<S>) this;
    }

    /**
     * Fetches the suggestion resolver linked to this
     * command parameter.
     *
     * @return the {@link SuggestionProvider} for a resolving suggestion
     */
    @Nullable
    SuggestionProvider<S> getSuggestionResolver();

    /**
     * Formats the usage parameter, the default value is the name of the parameter
     *
     * @see #name()
     * @return the formatted parameter
     */
    String format();

    /**
     * Sets a custom format for this parameter
     * the default value is the name of the parameter
     *
     * @see #name()
     * @param format the format to set
     */
    void setFormat(String format);

    default boolean isNumeric() {
        return this instanceof NumericParameter;
    }

    default NumericParameter<S> asNumeric() {
        return (NumericParameter<S>) this;
    }

    /**
     * Checks if this parameter has same name and valueType to the other {@link Argument}
     * unlike `Argument#equals(Object)`,
     * if both parameters are only different in their parent {@link Command},
     * it would still return true
     *
     * @param parameter the parameter to compare to
     * @return Whether this parameter has same name and valueType to the other {@link Argument} or not
     */
    boolean similarTo(Argument<?> parameter);

    default boolean isRequired() {
        return !isOptional();
    }

    /**
     * Creates a copy of this parameter with a different position.
     * Useful for commands that have multiple syntaxes.
     *
     * @param newPosition the new position to set
     * @return a copy of this parameter with the new position
     */
    Argument<S> copyWithDifferentPosition(int newPosition);

    @NotNull PriorityList<ArgValidator<S>> getValidators();

    void addValidator(@NotNull ArgValidator<S> validator);

    void validate(Context<S> context, ParsedArgument<S> parsedArgument) throws CommandException;
}
