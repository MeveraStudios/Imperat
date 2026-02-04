package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.type.ParameterArray;
import studio.mevera.imperat.command.parameters.type.ParameterCollection;
import studio.mevera.imperat.command.parameters.type.ParameterCommand;
import studio.mevera.imperat.command.parameters.type.ParameterMap;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.command.parameters.validator.InvalidArgumentException;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Argument;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.PriorityList;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public abstract class InputParameter<S extends Source> implements CommandParameter<S> {

    protected Command<S> parentCommand;
    protected final String name;
    protected String format;
    protected final ParameterType<S, ?> type;
    protected final boolean optional, flag, greedy;
    protected final OptionalValueSupplier optionalValueSupplier;
    protected final SuggestionResolver<S> suggestionResolver;
    protected String permission;
    protected Description description;
    protected int index;
    private final PriorityList<ArgValidator<S>> validators = new PriorityList<>();

    protected InputParameter(
        String name,
        @NotNull ParameterType<S, ?> type,
        @Nullable String permission,
        Description description,
        boolean optional, boolean flag, boolean greedy,
        @NotNull OptionalValueSupplier optionalValueSupplier,
        @Nullable SuggestionResolver<S> suggestionResolver
    ) {
        this.name = name;
        this.format = name;
        this.type = type;
        this.permission = permission;
        this.description = description;
        this.optional = optional;
        this.flag = flag;
        this.greedy = greedy;
        this.optionalValueSupplier = optionalValueSupplier;
        this.suggestionResolver = suggestionResolver;
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
    public @NotNull ParameterType<S, ?> type() {
        return type;
    }

    @Override
    public TypeWrap<?> wrappedType() {
        return type.wrappedType();
    }
    
    @Override
    public @Nullable String getSinglePermission() {
        return permission;
    }
    
    @Override
    public void setSinglePermission(String permission) {
        CommandParameter.super.setSinglePermission(permission);
    }
    
    /**
     * The permission for this parameter
     *
     * @return the parameter permission
     */
    @Override
    public @Unmodifiable Set<String> getPermissions() {
        if(permission == null) {
            return Collections.emptySet();
        }
        return Set.of(permission);
    }

    @Override
    public void addPermission(String permission) {
        this.permission = permission;
    }

    /**
     * @return the default value if it's input is not present
     * in case of the parameter being optional
     */
    @Override
    public @NotNull OptionalValueSupplier getDefaultValueSupplier() {
        return optionalValueSupplier.isEmpty() ? type.supplyDefaultValue() : optionalValueSupplier;
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
        return flag || this instanceof FlagParameter<?>;
    }

    /**
     * Casts the parameter to a flag parameter
     *
     * @return the parameter as a flag
     */
    @Override
    @SuppressWarnings("unchecked")
    public FlagParameter<S> asFlagParameter() {
        return (FlagParameter<S>) this;
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
        return greedy || (this.type instanceof ParameterCollection<?,?,?>)
                || (this.type instanceof ParameterArray<?,?>)
                || (this.type instanceof ParameterMap<?,?,?,?>);
    }

    @Override
    public boolean isGreedyString() {
        boolean isGreedyWrapper = ( TypeUtility.isAcceptableGreedyWrapper(this.type.type()) && TypeUtility.hasGenericType(type.type(), String.class));
        return ( this.type.equalsExactly(String.class) || isGreedyWrapper) && greedy;
    }

    @Override
    public Command<S> asCommand() {
        if(!(this.type instanceof ParameterCommand<?> asCommandType)) {
            throw new UnsupportedOperationException("Non-CommandProcessingChain Parameter cannot be converted into a command parameter");
        }
        return parentCommand.getSubCommand(asCommandType.getName());
    }


    /**
     * Fetches the suggestion resolver linked to this
     * command parameter.
     *
     * @return the {@link SuggestionResolver} for a resolving suggestion
     */
    @Override
    public @Nullable SuggestionResolver<S> getSuggestionResolver() {
        return suggestionResolver;
    }

    @Override
    public Description description() {
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
    public @NotNull PriorityList<ArgValidator<S>> getValidatorsQueue() {
        return validators.asUnmodifiable();
    }

    @Override
    public void validate(Context<S> context, Argument<S> argument) throws InvalidArgumentException {
        for (ArgValidator<S> validator : validators) {
            validator.validate(context, argument);
        }
    }

    @Override
    public boolean similarTo(CommandParameter<?> parameter) {
        return this.name.equalsIgnoreCase(parameter.name())
            && type.equalsExactly(parameter.wrappedType().getType());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputParameter<?> that)) return false;
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
