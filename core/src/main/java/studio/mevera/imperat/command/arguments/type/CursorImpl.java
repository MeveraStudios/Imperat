package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Default {@link Cursor} implementation: an immutable list of tokens plus a
 * mutable position. Snapshots reuse the same underlying list, so producing a
 * snapshot is allocation-cheap.
 */
final class CursorImpl<S extends CommandSource> implements Cursor<S> {

    private final CommandContext<S> context;
    private final List<String> tokens;
    private int position;

    CursorImpl(@NotNull CommandContext<S> context, @NotNull List<String> tokens, int position) {
        this.context = context;
        this.tokens = tokens;
        this.position = position;
    }

    @Override
    public boolean hasNext() {
        return position < tokens.size();
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public int size() {
        return tokens.size();
    }

    @Override
    public @Nullable String peek() {
        return position < tokens.size() ? tokens.get(position) : null;
    }

    @Override
    public @Nullable String peekAt(int offset) {
        int target = position + offset;
        return target >= 0 && target < tokens.size() ? tokens.get(target) : null;
    }

    @Override
    public @NotNull String next() {
        if (position >= tokens.size()) {
            throw new NoSuchElementException("Cursor is at end of input (size=" + tokens.size() + ")");
        }
        return tokens.get(position++);
    }

    @Override
    public @Nullable String nextOrNull() {
        return position < tokens.size() ? tokens.get(position++) : null;
    }

    @Override
    public @NotNull String collect(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative: " + count);
        }
        if (count == 0) {
            return "";
        }
        if (remaining() < count) {
            throw new NoSuchElementException(
                    "Cursor only has " + remaining() + " tokens left; cannot collect " + count
            );
        }
        StringBuilder builder = new StringBuilder(tokens.get(position));
        position++;
        for (int i = 1; i < count; i++) {
            builder.append(' ').append(tokens.get(position));
            position++;
        }
        return builder.toString();
    }

    @Override
    public @NotNull String collectRemaining() {
        if (position >= tokens.size()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(tokens.get(position));
        position++;
        while (position < tokens.size()) {
            builder.append(' ').append(tokens.get(position));
            position++;
        }
        return builder.toString();
    }

    @Override
    public @NotNull String slice(int from, int to) {
        if (from < 0 || to > tokens.size() || from > to) {
            throw new IndexOutOfBoundsException(
                    "slice(" + from + ", " + to + ") out of bounds for size " + tokens.size()
            );
        }
        if (from == to) {
            return "";
        }
        StringBuilder builder = new StringBuilder(tokens.get(from));
        for (int i = from + 1; i < to; i++) {
            builder.append(' ').append(tokens.get(i));
        }
        return builder.toString();
    }

    @Override
    public @NotNull Cursor<S> snapshot() {
        return new CursorImpl<>(context, tokens, position);
    }

    @Override
    public void commitFrom(@NotNull Cursor<S> other) {
        if (!(other instanceof CursorImpl<S> impl) || impl.tokens != this.tokens) {
            throw new IllegalArgumentException(
                    "commitFrom requires a snapshot of this cursor (different token sources)"
            );
        }
        this.position = impl.position;
    }

    @Override
    public @NotNull CommandContext<S> context() {
        return context;
    }
}
