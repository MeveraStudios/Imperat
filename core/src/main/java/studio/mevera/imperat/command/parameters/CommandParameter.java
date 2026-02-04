package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.parameters.AnnotatedParameter;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.DescriptionHolder;
import studio.mevera.imperat.command.PermissionHolder;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.command.parameters.validator.InvalidArgumentException;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Argument;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.Preconditions;
import studio.mevera.imperat.util.PriorityList;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.ArrayList;

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
        @NotNull OptionalValueSupplier valueSupplier,
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
    
    static <S extends Source> CommandParameter<S> literal(String part) {
        Preconditions.notNull(part, "part");
        Preconditions.checkArgument(!part.isEmpty(), "Literal part cannot be empty");
        Preconditions.checkArgument(part.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_'),
            "Literal part must be alphanumeric or underscore only");
        
        return of(
            part,
            ParameterTypes.command(part, new ArrayList<>()),
            null,
            Description.EMPTY,
            false,
            false,
            OptionalValueSupplier.empty(),
            null
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
     * For parameters, ONLY one permission is allowed for each parameter.
     * @return the single permission for this parameter.
     */
    @Nullable String getSinglePermission();
    
    default void setSinglePermission(String permission) {
        addPermission(getSinglePermission());
    }
    
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
    OptionalValueSupplier getDefaultValueSupplier();

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
    
    /**
     * Creates a copy of this parameter with a different position.
     * Useful for commands that have multiple syntaxes.
     *
     * @param newPosition the new position to set
     * @return a copy of this parameter with the new position
     */
    CommandParameter<S> copyWithDifferentPosition(int newPosition);

    @NotNull PriorityList<ArgValidator<S>> getValidatorsQueue();

    void addValidator(@NotNull ArgValidator<S> validator);

    void validate(Context<S> context, Argument<S> argument) throws InvalidArgumentException;
}
