package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * A focused, transactional input cursor handed to {@link ArgumentType#parse}
 * during command tree traversal. The cursor exposes the run of raw input
 * tokens that the tree has allocated to a specific argument and lets the
 * argument type consume them at its own pace.
 *
 * <p>Two design rules drive the API:
 * <ol>
 *   <li><b>Token-level only.</b> The cursor never exposes character-level
 *       navigation, parameter-list awareness, or pathway shape. Argument types
 *       that need character-level work (e.g. selector parsing) construct a
 *       {@code CharStream} from {@link #next()} themselves; the tree owns
 *       parameter-list and pathway concerns.</li>
 *   <li><b>Transactional snapshot/commit.</b> Argument types receive a
 *       detached {@link #snapshot}; advancing the snapshot does not affect
 *       the underlying stream. The tree commits the snapshot back via
 *       {@link #commitFrom} only on a successful parse, so a thrown exception
 *       cleanly rolls back partial advancement without any explicit reset.</li>
 * </ol>
 *
 * <p>The cursor's read range is bounded by a per-argument <b>token budget</b>
 * computed by the tree from {@link ArgumentType#getNumberOfParametersToConsume}
 * (or, for greedy types, from reservation/discrimination logic). Reading past
 * the budget yields {@code null} from {@link #peek}/{@link #nextOrNull} and
 * throws from {@link #next}. Argument types are expected to consume between 0
 * and {@code budget} tokens; the tree advances the underlying input by the
 * cursor's actual final position via {@link #commitFrom}.</p>
 *
 * <p>Implementations are <b>not thread-safe</b>.</p>
 *
 * @param <S> the command source type
 * @since 4.1.0
 */
public sealed interface Cursor<S extends CommandSource> permits CursorImpl {

    /**
     * Constructs a cursor over the given token list with the given budget.
     * Both arguments must come from the tree; argument types should not call
     * this directly.
     */
    static <S extends CommandSource> Cursor<S> of(
            @NotNull CommandContext<S> context,
            @NotNull List<String> tokens
    ) {
        return new CursorImpl<>(context, List.copyOf(tokens), 0);
    }

    /**
     * Convenience for delegating to an inner argument type with a single
     * already-known token (default-value parsing, value-flag parsing, etc.).
     * Returns a fresh cursor positioned at the start of the one-token list.
     */
    static <S extends CommandSource> Cursor<S> single(
            @NotNull CommandContext<S> context,
            @NotNull String token
    ) {
        return new CursorImpl<>(context, List.of(token), 0);
    }

    /**
     * @return {@code true} if at least one more token is available within the
     *         cursor's budget.
     */
    boolean hasNext();

    /**
     * @return the index of the next token to be read, relative to this cursor's
     *         start. Always non-negative; equal to {@link #size()} at end of
     *         input.
     */
    int position();

    /**
     * @return the total number of tokens visible to this cursor (its budget).
     */
    int size();

    /**
     * @return number of tokens still available, equivalent to
     *         {@code size() - position()}.
     */
    default int remaining() {
        return size() - position();
    }

    /**
     * Returns the token at the current position without advancing, or
     * {@code null} if at end of input.
     */
    @Nullable
    String peek();

    /**
     * Returns the token at {@code position() + offset} without advancing, or
     * {@code null} if that position is out of bounds.
     */
    @Nullable
    String peekAt(int offset);

    /**
     * Reads the token at the current position and advances.
     *
     * @throws NoSuchElementException if at end of input
     */
    @NotNull
    String next();

    /**
     * Reads the token at the current position and advances. Returns
     * {@code null} at end of input (the position does not change in that
     * case).
     */
    @Nullable
    String nextOrNull();

    /**
     * Joins the next {@code count} tokens with a single space and advances
     * past them. Throws if fewer tokens remain than requested.
     */
    @NotNull
    String collect(int count);

    /**
     * Joins all remaining tokens with a single space and advances to end of
     * input. Returns an empty string if no tokens remain.
     */
    @NotNull
    String collectRemaining();

    /**
     * Joins tokens in the half-open range {@code [from, to)} with a single
     * space, without changing the cursor position. Used by callers (typically
     * the tree) to build {@code ParseResult} input strings spanning the actual
     * consumed range.
     */
    @NotNull
    String slice(int from, int to);

    /**
     * Returns a detached copy of this cursor at the current position. Advancing
     * the snapshot does not affect this cursor.
     *
     * <p>The intended pattern is: callers (the tree) hand a snapshot to
     * {@link ArgumentType#parse}; on success, the caller commits the snapshot
     * back via {@link #commitFrom}; on a thrown exception the snapshot is
     * dropped and no advancement leaks back.</p>
     */
    @NotNull
    Cursor<S> snapshot();

    /**
     * Adopts the position of {@code other} into this cursor. Both cursors
     * must come from the same root via {@link #snapshot}; behaviour is
     * undefined otherwise.
     */
    void commitFrom(@NotNull Cursor<S> other);

    /**
     * @return the command context associated with the parse.
     */
    @NotNull
    CommandContext<S> context();
}
