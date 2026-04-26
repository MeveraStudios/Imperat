package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.lang.reflect.Type;

/**
 * Convenience base class for argument types that consume <b>all remaining
 * tokens</b> within their cursor budget, joined into a single space-separated
 * string. The framework gathers the budget tokens from the {@link Cursor} and
 * forwards them as one {@code String} to
 * {@link #parse(CommandContext, Argument, String)}.
 *
 * <p>The cursor budget passed in by the tree already accounts for:
 * <ul>
 *   <li>The argument's {@code @Greedy(limit = N)} cap.</li>
 *   <li>Reservation for downstream required arguments (so a greedy never
 *       starves a trailing required arg of input).</li>
 *   <li>Type-discrimination yield (a downstream arg with a discriminating
 *       type — e.g. {@code int} after a greedy String — pulls back tokens it
 *       can parse).</li>
 *   <li>Inline registered flags (extracted from the greedy span and bound
 *       independently by the tree).</li>
 * </ul>
 * Implementers therefore see a clean joined input that is exactly what should
 * become the greedy value.</p>
 *
 * <p>Use this for "rest of the line" arguments such as chat messages, ban
 * reasons, or natural-language inputs. For single-token arguments, extend
 * {@link SimpleArgumentType}; for full cursor control extend
 * {@link ArgumentType} directly.</p>
 *
 * <p>The {@link #isGreedy} flag is fixed to {@code true} and {@link #getNumberOfParametersToConsume}
 * returns {@code -1} (unbounded — limit is enforced via the
 * {@code @Greedy(limit = N)} annotation honoured by the tree). Both are
 * {@code final}.</p>
 *
 * @param <S> the command source type
 * @param <T> the parsed value type (typically {@code String})
 */
public abstract class GreedyArgumentType<S extends CommandSource, T> extends ArgumentType<S, T> {

    public GreedyArgumentType() {
        super();
    }

    public GreedyArgumentType(Class<T> type) {
        super(type);
    }

    public GreedyArgumentType(Type type) {
        super(type);
    }

    /**
     * Drains the cursor's remaining tokens into a single space-separated
     * string and forwards to {@link #parse(CommandContext, Argument, String)}.
     */
    @Override
    public final T parse(
            @NotNull CommandContext<S> context,
            @NotNull Argument<S> argument,
            @NotNull Cursor<S> cursor
    ) throws CommandException {
        String joined = cursor.collectRemaining();
        return parse(context, argument, joined);
    }

    /**
     * Parse the joined-token greedy input into the typed value.
     *
     * @param context     the execution / command context
     * @param argument    the argument descriptor
     * @param joinedInput the budget tokens joined with a single space (may be
     *                    empty if the budget had no tokens)
     * @return the resolved value of type T
     * @throws CommandException if parsing fails
     */
    public abstract T parse(
            @NotNull CommandContext<S> context,
            @NotNull Argument<S> argument,
            @NotNull String joinedInput
    ) throws CommandException;

    @Override
    public final boolean isGreedy(Argument<S> parameter) {
        return true;
    }

    @Override
    public final int getNumberOfParametersToConsume(Argument<S> argument) {
        return -1;
    }
}
