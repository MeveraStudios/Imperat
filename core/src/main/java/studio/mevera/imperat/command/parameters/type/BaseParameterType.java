package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.TypeCapturer;
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
public abstract class BaseParameterType<S extends Source, T> 
    extends TypeCapturer implements ParameterType<S, T> {

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
     * Constructs a new BaseParameterType with an automated {@link TypeWrap}
     */
    public BaseParameterType() {
        this.type = this.extractType(BaseParameterType.class, 1);
    }

    /**
     * Constructs a new BaseParameterType with the given TypeWrap.
     *
     * @param type The type of the parameter.
     */
    public BaseParameterType(final Class<T> type) {
        this.type = type;
    }

    /**
     * Constructs a new BaseParameterType with the given TypeWrap.
     *
     * @param type The type of the parameter.
     */
    public BaseParameterType(final Type type) {
        this.type = type;
    }

    /**
     * Retrieves the Type associated with this BaseParameterType.
     *
     * @return the Type associated with this parameter type.
     */
    @Override
    public Type type() {
        return type;
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionResolver<S> getSuggestionResolver() {
        return suggestions.isEmpty() ?  null : SuggestionResolver.staticSuggestions(suggestions);
    }

    /**
     * Determines whether the provided input matches the expected format or criteria
     * for a given command parameter. this is used during {@link CommandTree#contextMatch(Source, ArgumentInput)}
     *
     * @param input     The input string to be matched against the parameter criteria.
     * @param parameter The command parameter that provides context for the input handling.
     * @return true if the input matches the expected criteria; false otherwise.
     */
    @Override
    public boolean matchesInput(String input, CommandParameter<S> parameter) {
        return true;
    }

    /**
     * Adds the provided suggestions to the current list of suggestions for this parameter type.
     *
     * @param suggestions The array of suggestions to be added.
     * @return The current instance of the parameter type with the added suggestions.
     */
    @Override
    public @NotNull ParameterType<S, T> withSuggestions(String... suggestions) {
        this.suggestions.addAll(List.of(suggestions));
        return this;
    }

}
