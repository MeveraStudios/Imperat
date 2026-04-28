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
 * <h2>Design rules</h2>
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
 * <h2>The token budget</h2>
 * The cursor's read range is bounded by a per-argument <b>token budget</b>
 * computed by the tree from {@link ArgumentType#getNumberOfParametersToConsume}
 * (or, for greedy types, from reservation/discrimination logic). Reading past
 * the budget yields {@code null} from {@link #peek}/{@link #nextOrNull} and
 * throws from {@link #next}. Argument types are expected to consume between 0
 * and {@code budget} tokens; the tree advances the underlying input by the
 * cursor's actual final position via {@link #commitFrom}.
 *
 * <h2>Snapshot / commit lifecycle</h2>
 * The framework's parse entry point follows this protocol:
 * <pre>{@code
 * Cursor<S> root = Cursor.of(ctx, allocatedTokens); // tree-side
 * Cursor<S> probe = root.snapshot();                // detached copy
 * try {
 *     T value = type.parse(ctx, arg, probe);       // user-side advances probe
 *     root.commitFrom(probe);                      // success → adopt position
 * } catch (CommandException ex) {
 *     // failure → drop probe, root keeps its original position
 * }
 * }</pre>
 * The {@code probe} given to user code shares the same immutable token list
 * as {@code root} but carries an independent mutable position. {@link #commitFrom}
 * verifies the same-source rule and copies the position over. Throwing from
 * {@code parse} cleanly rolls back: the framework simply discards {@code probe}.
 *
 * <h2>Thread-safety contract</h2>
 * Implementations are <b>not thread-safe</b>. A cursor (including any
 * {@link #snapshot}) is bound to the thread that received it from the tree
 * and must not be shared with worker threads. In particular:
 * <ul>
 *   <li><b>Do not hand a cursor or its snapshot to another thread.</b> Even
 *       though {@link #snapshot} produces a detached position, the parent
 *       cursor is also referenced by the tree, which reads {@link #position}
 *       immediately after {@code parse} returns to know how far to advance
 *       the underlying input stream. Worker mutations would race with that
 *       read and silently steal tokens from downstream arguments.</li>
 *   <li><b>For async / off-thread work, drain into a {@code List<String>}
 *       on the calling thread first</b>, then construct a fresh cursor for
 *       the worker via {@link #of}:
 *       <pre>{@code
 *       List<String> drained = new ArrayList<>();
 *       int n = innerType.getNumberOfParametersToConsume(arg);
 *       for (int i = 0; i < n && cursor.hasNext(); i++) drained.add(cursor.next());
 *       CompletableFuture.supplyAsync(() ->
 *               innerType.parse(ctx, arg, Cursor.of(ctx, drained)));
 *       }</pre>
 *       This is exactly how {@code CompletableFutureArgument} hands work to
 *       a worker — the calling thread settles the budget commitment, the
 *       worker gets an isolated cursor over a frozen token list.</li>
 *   <li><b>Same applies to default-value resolution and value-flag parsing</b>
 *       — both construct dedicated cursors via {@link #single} / {@link #of}
 *       rather than reusing the parent walk's cursor.</li>
 * </ul>
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
     * Returns a detached copy of this cursor at the current position. The
     * snapshot shares the same immutable token list as the parent but holds
     * its own mutable position; advancing the snapshot does not affect this
     * cursor.
     *
     * <p>The intended pattern is: callers (the tree) hand a snapshot to
     * {@link ArgumentType#parse}; on success, the caller commits the snapshot
     * back via {@link #commitFrom}; on a thrown exception the snapshot is
     * dropped and no advancement leaks back.</p>
     *
     * <p><b>Threading:</b> the snapshot is bound to the calling thread for
     * the same reason the parent cursor is — see the class-level
     * "Thread-safety contract" section. Do not pass a snapshot to a worker
     * thread; drain the tokens you need into a {@code List<String>} on the
     * calling thread and re-build a fresh cursor via {@link #of} for the
     * worker instead.</p>
     */
    @NotNull
    Cursor<S> snapshot();

    /**
     * Adopts the position of {@code other} into this cursor. Both cursors
     * must come from the same root via {@link #snapshot}; calling with a
     * cursor produced by an unrelated {@link #of} or {@link #single} factory
     * throws {@link IllegalArgumentException}.
     *
     * <p>Used by the framework after a successful {@code parse} to advance
     * the underlying input stream by the number of tokens the argument type
     * actually consumed. A failed parse simply omits the {@code commitFrom}
     * call — the original cursor's position is unchanged, providing
     * exception-safe rollback.</p>
     */
    void commitFrom(@NotNull Cursor<S> other);

    /**
     * @return the command context associated with the parse.
     */
    @NotNull
    CommandContext<S> context();
}
