package dev.velix.imperat.command.parameters;

import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.Description;
import dev.velix.imperat.command.parameters.type.ArrayParameterType;
import dev.velix.imperat.command.parameters.type.CollectionParameterType;
import dev.velix.imperat.command.parameters.type.MapParameterType;
import dev.velix.imperat.command.parameters.type.ParameterCommand;
import dev.velix.imperat.command.parameters.type.ParameterType;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.resolvers.SuggestionResolver;
import dev.velix.imperat.supplier.OptionalValueSupplier;
import dev.velix.imperat.util.TypeWrap;
import org.jetbrains.annotations.*;

import java.util.Objects;

@ApiStatus.Internal
public abstract class InputParameter<S extends Source> implements CommandParameter<S> {

    protected Command<S> parentCommand;
    protected final String name;
    protected final ParameterType<S, ?> type;
    protected final boolean optional, flag, greedy;
    protected final OptionalValueSupplier<?> optionalValueSupplier;
    protected final SuggestionResolver<S> suggestionResolver;
    protected String permission;
    protected Description description;
    protected int index;


    protected InputParameter(
        String name,
        @NotNull ParameterType<S, ?> type,
        @Nullable String permission,
        Description description,
        boolean optional, boolean flag, boolean greedy,
        @NotNull OptionalValueSupplier<?> optionalValueSupplier,
        @Nullable SuggestionResolver<S> suggestionResolver
    ) {
        this.name = name;
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

    /**
     * The permission for this parameter
     *
     * @return the parameter permission
     */
    @Override
    public @Nullable String permission() {
        return permission;
    }

    @Override
    public void permission(String permission) {
        this.permission = permission;
    }

    /**
     * @return the default value if it's input is not present
     * in case of the parameter being optional
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull OptionalValueSupplier<T> getDefaultValueSupplier() {
        return (OptionalValueSupplier<T>) optionalValueSupplier;
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
        return flag;
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
        return greedy || (this.type instanceof CollectionParameterType<?,?,?>)
                || (this.type instanceof ArrayParameterType<?,?>)
                || (this.type instanceof MapParameterType<?,?,?,?>);
    }

    @Override
    public boolean isGreedyString() {
        return this.type.equalsExactly(String.class) && greedy;
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
