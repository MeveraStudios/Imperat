package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.lang.reflect.Type;

/**
 * Convenience base class for argument types that consume a <b>fixed number</b>
 * of raw input tokens and parse them as a single joined {@link String}. The
 * framework reads {@link #getNumberOfParametersToConsume} tokens off the
 * {@link Cursor}, joins them with a single space, and forwards the result to
 * {@link #parse(CommandContext, Argument, String)} — sparing the implementer
 * any cursor handling.
 *
 * <p>The default arity is one token (the overwhelming majority of types:
 * numerics, booleans, enums, names, IDs, single-word semantics). Pass a
 * different value via {@link #SimpleArgumentType(int)} (or its overloads) to
 * consume multiple tokens — useful for types whose textual form is
 * whitespace-separated but whose token count is fixed (e.g. a 3-token
 * coordinate triple, a 2-token ISO date+time pair).</p>
 *
 * <p>For variable-arity "rest of the line" inputs, extend
 * {@link GreedyArgumentType} instead. For full cursor control (peek-driven
 * parsing, optional consumption, etc.), extend {@link ArgumentType} directly.</p>
 *
 * <p>The {@link #getNumberOfParametersToConsume} and {@link #isGreedy}
 * properties are fixed at construction: a {@code SimpleArgumentType} consumes
 * its configured number of tokens, never greedy. Both methods are {@code final}
 * to enforce the contract.</p>
 *
 * @param <S> the command source type
 * @param <T> the parsed value type
 */
public abstract class SimpleArgumentType<S extends CommandSource, T> extends ArgumentType<S, T> {

    private static final int DEFAULT_NUMBER_OF_PARAMETERS = 1;

    private final int numberOfParameters;

    public SimpleArgumentType() {
        this(DEFAULT_NUMBER_OF_PARAMETERS);
    }

    public SimpleArgumentType(Class<T> type) {
        this(type, DEFAULT_NUMBER_OF_PARAMETERS);
    }

    public SimpleArgumentType(Type type) {
        this(type, DEFAULT_NUMBER_OF_PARAMETERS);
    }

    public SimpleArgumentType(int numberOfParameters) {
        super();
        this.numberOfParameters = validate(numberOfParameters);
    }

    public SimpleArgumentType(Class<T> type, int numberOfParameters) {
        super(type);
        this.numberOfParameters = validate(numberOfParameters);
    }

    public SimpleArgumentType(Type type, int numberOfParameters) {
        super(type);
        this.numberOfParameters = validate(numberOfParameters);
    }

    private static int validate(int numberOfParameters) {
        if (numberOfParameters < 1) {
            throw new IllegalArgumentException(
                    "SimpleArgumentType numberOfParameters must be >= 1 (got "
                            + numberOfParameters
                            + "); use GreedyArgumentType for variable-arity, or ArgumentType for zero/optional consumption."
            );
        }
        return numberOfParameters;
    }

    /**
     * Reads the configured number of tokens allocated to this argument from
     * {@code cursor}, joins them with a single space, and forwards the result
     * to {@link #parse(CommandContext, Argument, String)}.
     */
    @Override
    public final T parse(
            @NotNull CommandContext<S> context,
            @NotNull Argument<S> argument,
            @NotNull Cursor<S> cursor
    ) throws CommandException {
        if (cursor.remaining() < numberOfParameters) {
            throw new IllegalArgumentException(
                    "Argument type '" + getClass().getSimpleName()
                            + "' expected " + numberOfParameters
                            + " input token(s) but the cursor has " + cursor.remaining()
                            + " remaining"
            );
        }
        String joined = cursor.collect(numberOfParameters);
        return parse(context, argument, joined);
    }

    /**
     * Parse the joined input tokens (one for default arity, multiple
     * space-joined when the argument type is configured for higher arity)
     * into the typed value.
     *
     * @param context  the execution / command context
     * @param argument the argument descriptor
     * @param input    the joined raw token(s) (never null, never blank under normal use)
     * @return the resolved value of type T
     * @throws CommandException if parsing fails
     */
    public abstract T parse(
            @NotNull CommandContext<S> context,
            @NotNull Argument<S> argument,
            @NotNull String input
    ) throws CommandException;

    @Override
    public final int getNumberOfParametersToConsume(Argument<S> argument) {
        return numberOfParameters;
    }

    @Override
    public final boolean isGreedy(Argument<S> parameter) {
        return false;
    }
}
