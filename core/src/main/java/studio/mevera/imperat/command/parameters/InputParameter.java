package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArrayArgument;
import studio.mevera.imperat.command.parameters.type.CollectionArgument;
import studio.mevera.imperat.command.parameters.type.CommandArgument;
import studio.mevera.imperat.command.parameters.type.MapArgument;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.PriorityList;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Objects;

@ApiStatus.Internal
public abstract class InputParameter<S extends Source> implements Argument<S> {

    protected final String name;
    protected final ArgumentType<S, ?> type;
    protected final boolean optional, flag, greedy;
    protected final DefaultValueProvider defaultValueProvider;
    protected final SuggestionProvider<S> suggestionProvider;
    private final PriorityList<ArgValidator<S>> validators = new PriorityList<>();
    protected Command<S> parentCommand;
    protected String format;
    protected PermissionsData permissionsData;
    protected Description description;
    protected int index;

    protected InputParameter(
            String name,
            @NotNull ArgumentType<S, ?> type,
            @NotNull PermissionsData permissionsData,
            Description description,
            boolean optional, boolean flag, boolean greedy,
            @NotNull DefaultValueProvider defaultValueProvider,
            @Nullable SuggestionProvider<S> suggestionProvider
    ) {
        this.name = name;
        this.format = name;
        this.type = type;
        this.permissionsData = permissionsData;
        this.description = description;
        this.optional = optional;
        this.flag = flag;
        this.greedy = greedy;
        this.defaultValueProvider = defaultValueProvider;
        this.suggestionProvider = suggestionProvider;
    }


    /**
     * @return the name of the parameter
     */
    @Override
    public String name() {
        return name;
    }

    @Override
    public String format() {
        return format;
    }

    @Override
    public final void setFormat(String format) {
        this.format = format;
    }

    @Override
    public @Nullable Command<S> parent() {
        return parentCommand;
    }

    @Override
    public void parent(@NotNull Command<S> parentCommand) {
        this.parentCommand = parentCommand;
    }


    /**
     * @return the index of this parameter
     */
    @Override
    public int position() {
        return index;
    }

    /**
     * Sets the position of this parameter in a syntax
     * DO NOT USE THIS FOR ANY REASON unless it's necessary to do so
     *
     * @param position the position to set
     */
    @Override
    public void position(int position) {
        this.index = position;
    }

    @Override
    public @NotNull ArgumentType<S, ?> type() {
        return type;
    }

    @Override
    public TypeWrap<?> wrappedType() {
        return type.wrappedType();
    }

    @Override
    public @NotNull PermissionsData getPermissionsData() {
        return permissionsData;
    }

    @Override
    public void setPermissionData(@NotNull PermissionsData permission) {
        this.permissionsData = permission;
    }

    /**
     * @return the default value if it's input is not present
     * in case of the parameter being optional
     */
    @Override
    public @NotNull DefaultValueProvider getDefaultValueSupplier() {
        return defaultValueProvider.isEmpty() ? type.getDefaultValueProvider() : defaultValueProvider;
    }

    /**
     * @return whether this is an optional argument
     */
    @Override
    public boolean isOptional() {
        return optional;
    }

    /**
     * @return checks whether this parameter is a flag
     */
    @Override
    public boolean isFlag() {
        return flag || this instanceof FlagArgument<?>;
    }

    /**
     * Casts the parameter to a flag parameter
     *
     * @return the parameter as a flag
     */
    @Override
    @SuppressWarnings("unchecked")
    public FlagArgument<S> asFlagParameter() {
        return (FlagArgument<S>) this;
    }

    /**
     * @return checks whether this parameter
     * consumes all the args input after it.
     */
    @Override
    public boolean isGreedy() {
        /*if ( (this.type.type() != String.class) && greedy) {
            throw new IllegalStateException(
                String.format("Usage parameter '%s' cannot be greedy while having value-valueType '%s'", name, valueType().getTypeName())
            );
        }*/
        return greedy || (this.type instanceof CollectionArgument<?, ?, ?>)
                       || (this.type instanceof ArrayArgument<?, ?>)
                       || (this.type instanceof MapArgument<?, ?, ?, ?>);
    }

    @Override
    public boolean isGreedyString() {
        boolean isGreedyWrapper = (TypeUtility.isAcceptableGreedyWrapper(this.type.type()) && TypeUtility.hasGenericType(type.type(), String.class));
        return (this.type.equalsExactly(String.class) || isGreedyWrapper) && greedy;
    }

    @Override
    public Command<S> asCommand() {
        if (!(this.type instanceof CommandArgument<?> asCommandType)) {
            throw new UnsupportedOperationException("Non-CommandProcessingChain Parameter cannot be converted into a command parameter");
        }
        return parentCommand.getSubCommand(asCommandType.getName());
    }


    /**
     * Fetches the suggestion resolver linked to this
     * command parameter.
     *
     * @return the {@link SuggestionProvider} for a resolving suggestion
     */
    @Override
    public @Nullable SuggestionProvider<S> getSuggestionResolver() {
        return suggestionProvider;
    }

    @Override
    public Description getDescription() {
        return description;
    }

    @Override
    public void describe(final Description description) {
        this.description = description;
    }

    @Override
    public void addValidator(@NotNull ArgValidator<S> validator) {
        validators.add(validator);
    }

    @Override
    public @NotNull PriorityList<ArgValidator<S>> getValidators() {
        return validators.asUnmodifiable();
    }

    @Override
    public void validate(ExecutionContext<S> context, ParsedArgument<S> parsedArgument) throws CommandException {
        for (ArgValidator<S> validator : validators) {
            validator.validate(context, parsedArgument);
        }
    }

    @Override
    public boolean similarTo(Argument<?> parameter) {
        return this.name.equalsIgnoreCase(parameter.name())
                       && type.equalsExactly(parameter.wrappedType().getType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InputParameter<?> that)) {
            return false;
        }
        return Objects.equals(parentCommand, that.parentCommand)
                       && Objects.equals(name, that.name)
                       && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return format();
    }

}
