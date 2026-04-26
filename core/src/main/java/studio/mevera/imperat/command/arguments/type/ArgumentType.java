package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.DefaultValueProvider;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.TypeCapturer;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.priority.Prioritizable;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for defining parameter types in a command processing framework.
 *
 * <p>Argument types parse raw input tokens into typed values. The parse
 * contract is a single method, {@link #parse(CommandContext, Argument, Cursor)},
 * which receives a transactional {@link Cursor} over the input tokens the
 * command tree has allocated to this argument. The cursor is detached: the
 * tree commits a successful parse's advancement back to the underlying input
 * stream and silently rolls back on a thrown {@link CommandException}.</p>
 *
 * <p>For the common single-token case, prefer extending
 * {@link SimpleArgumentType} — it handles the cursor and exposes a String
 * input. For greedy types that consume all remaining tokens (with optional
 * limit and downstream-aware reservation), extend {@link GreedyArgumentType}.
 * Extend this class directly only when you need full cursor control: peek,
 * consume a variable number of tokens based on input shape, etc.</p>
 *
 * @param <S> The type of the source from which the command originates.
 * @param <T> The type of the parameter being handled.
 */
public abstract class ArgumentType<S extends CommandSource, T>
        extends TypeCapturer implements Prioritizable {

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
    private SuggestionProvider<S> cachedSuggestionProvider;

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
     * Parses the argument value by reading from {@code cursor}.
     *
     * <p>The cursor is a detached snapshot; the tree commits the cursor's
     * final position back to the underlying input stream only on a successful
     * return. Throwing any {@link CommandException} signals a parse failure
     * and rolls back any tokens this type consumed from the cursor.</p>
     *
     * <p>Implementations should consume the tokens that comprise this
     * argument's value and return the parsed result. Tokens not consumed
     * within this argument's budget are returned to the input stream for
     * subsequent arguments.</p>
     *
     * @param context  the execution / command context.
     * @param argument the argument descriptor (name, position, modifiers).
     * @param cursor   a transactional cursor over the input tokens allocated
     *                 to this argument.
     * @return the resolved value of type T.
     * @throws CommandException if parsing fails.
     */
    public abstract T parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull Cursor<S> cursor) throws CommandException;

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    public SuggestionProvider<S> getSuggestionProvider() {
        if (suggestions.isEmpty()) {
            return null;
        }
        if (cachedSuggestionProvider == null) {
            cachedSuggestionProvider = SuggestionProvider.staticSuggestions(suggestions);
        }
        return cachedSuggestionProvider;
    }

    /**
     * Returns the default value supplier for the given source and command parameter.
     * By default, this returns an empty supplier, indicating no default value.
     *
     * @return an {@link DefaultValueProvider} providing the default value, or empty if none.
     */
    @ApiStatus.AvailableSince("1.9.1")
    public DefaultValueProvider getDefaultValueProvider() {
        return DefaultValueProvider.empty();
    }

    /**
     * Checks if the given type is related to this parameter type.
     *
     * @param type the type to check.
     * @return {@code true} if the types are related, {@code false} otherwise.
     */
    public final boolean isRelatedToType(Type type) {
        return TypeUtility.areRelatedTypes(type, this.type());
    }

    /**
     * Checks if the given type exactly matches this parameter type.
     *
     * @param type the type to check.
     * @return {@code true} if the types match exactly, {@code false} otherwise.
     */
    public final boolean equalsExactly(Type type) {
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
    public int getNumberOfParametersToConsume(Argument<S> argument) {
        return 1;
    }

    /**
     * Adds the provided suggestions to the current list of suggestions for this parameter type.
     *
     * @param suggestions The array of suggestions to be added.
     */
    public void addStaticSuggestions(String... suggestions) {
        this.suggestions.addAll(List.of(suggestions));
        this.cachedSuggestionProvider = null;
    }

    @ApiStatus.AvailableSince("3.0.0")
    @Override
    public @NotNull Priority getPriority() {
        return Priority.NORMAL;
    }
}
