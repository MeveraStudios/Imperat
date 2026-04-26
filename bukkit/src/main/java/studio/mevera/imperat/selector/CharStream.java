package studio.mevera.imperat.selector;

import org.jetbrains.annotations.Nullable;

/**
 * A minimal character-level cursor over an input string, intended for the
 * focused, domain-specific parsing required by Minecraft target-selector
 * expressions (e.g. {@code @a[tag=foo,distance=5]}).
 *
 * <p>This deliberately replaces the previous reliance on the framework-wide
 * {@code Cursor} type for selector parsing: the selector path only ever needs
 * to peek, advance, and collect runs of characters terminated by a delimiter
 * — none of {@code Cursor}'s parameter-aware navigation. Carrying a small,
 * single-purpose abstraction here keeps the selector code free of any
 * coupling to the parsing pipeline used by the command tree.</p>
 *
 * <p>Three operation groups:
 * <ul>
 *   <li><b>Inspect</b> ({@link #peek()}, {@link #peekNext()}, {@link #hasNext()})
 *       — read without advancing.</li>
 *   <li><b>Advance</b> ({@link #next()}, {@link #skip()}) — read and move forward.</li>
 *   <li><b>Collect</b> ({@link #collectUntil(char)}, {@link #skipUntil(char)},
 *       {@link #remaining()}) — bulk character operations terminated by a
 *       delimiter or end-of-input.</li>
 * </ul>
 *
 * <p>Not thread-safe. Indices are zero-based; {@link #position()} returns the
 * index of the next character to be read.</p>
 */
public final class CharStream {

    private final String source;
    private int position;

    public CharStream(String source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.source = source;
        this.position = 0;
    }

    /**
     * Returns the underlying input string.
     */
    public String source() {
        return source;
    }

    /**
     * Returns the index of the next character to be read.
     */
    public int position() {
        return position;
    }

    /**
     * Returns the total length of the input string.
     */
    public int length() {
        return source.length();
    }

    /**
     * Returns {@code true} if at least one more character is available.
     */
    public boolean hasNext() {
        return position < source.length();
    }

    /**
     * Returns the character at the current position without advancing,
     * or {@code null} if at end-of-input.
     */
    public @Nullable Character peek() {
        return position < source.length() ? source.charAt(position) : null;
    }

    /**
     * Returns the character one past the current position without advancing,
     * or {@code null} if no such character exists.
     */
    public @Nullable Character peekNext() {
        int next = position + 1;
        return next < source.length() ? source.charAt(next) : null;
    }

    /**
     * Reads the character at the current position and advances. Returns
     * {@code null} at end-of-input (the position is unchanged in that case).
     */
    public @Nullable Character next() {
        if (position >= source.length()) {
            return null;
        }
        return source.charAt(position++);
    }

    /**
     * Advances past the current character. Returns {@code true} if the
     * cursor moved, {@code false} if already at end-of-input.
     */
    public boolean skip() {
        if (position >= source.length()) {
            return false;
        }
        position++;
        return true;
    }

    /**
     * Advances past every character up to and including the first occurrence
     * of {@code target}. Returns {@code true} if the target was found and
     * skipped; {@code false} if end-of-input was reached without seeing it.
     */
    public boolean skipUntil(char target) {
        while (position < source.length()) {
            if (source.charAt(position++) == target) {
                return true;
            }
        }
        return false;
    }

    /**
     * Collects characters from the current position up to (but not including)
     * the first occurrence of {@code target}. The cursor is left pointing AT
     * the target character; if end-of-input is reached without seeing it, the
     * cursor is left at end-of-input and the entire remaining tail is returned.
     */
    public String collectUntil(char target) {
        int start = position;
        while (position < source.length() && source.charAt(position) != target) {
            position++;
        }
        return source.substring(start, position);
    }

    /**
     * Returns the remaining unread substring without advancing the cursor.
     */
    public String remaining() {
        return source.substring(position);
    }
}
