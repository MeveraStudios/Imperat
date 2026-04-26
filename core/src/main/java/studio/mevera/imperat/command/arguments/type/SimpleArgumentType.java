package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.lang.reflect.Type;

/**
 * Convenience base class for argument types that consume <b>exactly one</b>
 * raw input token. The framework reads the single token off the
 * {@link Cursor} and forwards it as a {@code String} to
 * {@link #parse(CommandContext, Argument, String)}, sparing the implementer
 * any cursor handling.
 *
 * <p>Use this for the overwhelming majority of argument types (numerics,
 * booleans, enums, names, IDs, single-word semantics). For types that
 * consume zero, several, or a variable number of tokens, extend
 * {@link ArgumentType} directly; for whitespace-joined "rest of the line"
 * inputs, extend {@link GreedyArgumentType}.</p>
 *
 * <p>The {@link #getNumberOfParametersToConsume} and {@link #isGreedy}
 * properties are fixed: a {@code SimpleArgumentType} always consumes exactly
 * one token and is never greedy. Both methods are {@code final} to enforce
 * the contract.</p>
 *
 * @param <S> the command source type
 * @param <T> the parsed value type
 */
public abstract class SimpleArgumentType<S extends CommandSource, T> extends ArgumentType<S, T> {

    public SimpleArgumentType() {
        super();
    }

    public SimpleArgumentType(Class<T> type) {
        super(type);
    }

    public SimpleArgumentType(Type type) {
        super(type);
    }

    /**
     * Reads the single token allocated to this argument from {@code cursor}
     * and forwards it as a String to {@link #parse(CommandContext, Argument, String)}.
     */
    @Override
    public final T parse(
            @NotNull CommandContext<S> context,
            @NotNull Argument<S> argument,
            @NotNull Cursor<S> cursor
    ) throws CommandException {
        String token = cursor.nextOrNull();
        if (token == null) {
            throw new IllegalArgumentException(
                    "Argument type '" + getClass().getSimpleName()
                            + "' expected one input token but the cursor is at end of input"
            );
        }
        return parse(context, argument, token);
    }

    /**
     * Parse a single already-extracted input token into the typed value.
     *
     * @param context  the execution / command context
     * @param argument the argument descriptor
     * @param input    the raw token (never null, never blank under normal use)
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
        return 1;
    }

    @Override
    public final boolean isGreedy(Argument<S> parameter) {
        return false;
    }
}
