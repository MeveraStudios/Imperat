package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Centralised drain logic for value-flag tokens. The legacy parse path
 * unconditionally consumed exactly one token per value-flag span (via
 * {@code remaining.next()}); that silently broke any value flag whose inner
 * type declares a multi-token arity (e.g. a 3-token coordinate triple bound
 * to a {@code -coords} flag).
 *
 * <p>This helper computes the drain size from
 * {@link ArgumentType#getNumberOfParametersToConsume} across the extracted
 * value-flag set (taking the max so multi-flag spans share a span large
 * enough for any of them) and packages the resulting tokens for handoff to
 * each flag's inner-type parse.</p>
 *
 * <p>Used by all three flag-value entry points: the in-pathway
 * {@link Node#parseOptionalsAndFlags}, the trailing-flag
 * {@code TreeParser.consumeRemainingFlags}, and the completion-path
 * {@code TreeSuggester.parseNodeForCompletion} mirror.</p>
 */
public final class FlagValueDrain {

    private FlagValueDrain() {
    }

    /**
     * Computes the maximum value-token count across the extracted flag set.
     * Returns {@code 0} if no extracted flag accepts a value (everything is
     * a switch).
     */
    public static <S extends CommandSource> int requiredTokenCount(@NotNull Collection<FlagArgument<S>> extracted) {
        int max = 0;
        for (FlagArgument<S> flag : extracted) {
            if (flag.isSwitch()) {
                continue;
            }
            ArgumentType<S, ?> inputType = flag.flagData().inputType();
            if (inputType == null) {
                max = Math.max(max, 1);
                continue;
            }
            max = Math.max(max, Math.max(1, inputType.getNumberOfParametersToConsume(flag)));
        }
        return max;
    }

    /**
     * Pulls {@code count} tokens off {@code stream} (advancing it), returning
     * them as an immutable list. Returns an empty list (no advance) if the
     * stream lacks enough tokens — caller treats that as "value missing" and
     * fails the value flag.
     */
    public static <S extends CommandSource> @NotNull List<String> drain(@NotNull RawInputStream<S> stream, int count) {
        if (count <= 0) {
            return List.of();
        }
        if (stream.remaining() < count) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tokens.add(stream.next());
        }
        return tokens;
    }

    /**
     * Joined display form of the drained value tokens — used as the
     * {@code rawInput} carried on a {@link ParseResult} so error messages
     * include the full multi-token span the user typed.
     */
    public static @NotNull String join(@Nullable List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        return String.join(" ", tokens);
    }

    /**
     * Builds the cursor handed to the value flag's inner type. Always backed
     * by the drained list so the inner type can use multi-token consumption
     * (via {@link Cursor#collect} / repeated {@link Cursor#next}) without
     * leaking into the outer parse stream.
     */
    public static <S extends CommandSource> @NotNull Cursor<S> cursor(@NotNull CommandContext<S> ctx, @NotNull List<String> tokens) {
        return Cursor.of(ctx, tokens);
    }
}
