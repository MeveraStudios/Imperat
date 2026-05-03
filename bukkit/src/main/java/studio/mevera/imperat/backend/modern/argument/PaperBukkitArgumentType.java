package studio.mevera.imperat.backend.modern.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;

import java.lang.reflect.Type;

/**
 * Imperat-side wrapper around a Paper Brigadier argument type. Drains
 * the cursor's tokens, joins them, and feeds them to the underlying
 * Paper {@link com.mojang.brigadier.arguments.ArgumentType}'s
 * {@code parse(StringReader)} — then applies the {@link PaperArgumentType}'s
 * resolver to produce the friendly Java type the command method expects.
 *
 * <p>This is the Paper-module counterpart to the legacy
 * {@code BukkitArgumentType}; it differs in that it carries no NMS
 * reflection and uses Paper's stable Brigadier API directly.</p>
 *
 * @param <N> Brigadier-native parsed form
 * @param <T> friendly Java type
 *
 * @since 4.0.0 (Paper module)
 */
public class PaperBukkitArgumentType<N, T> extends ArgumentType<BukkitCommandSource, T>
        implements PaperNativeArgumentType {

    private final PaperArgumentType<N, T> paperType;

    public PaperBukkitArgumentType(@NotNull Type type, @NotNull PaperArgumentType<N, T> paperType) {
        super(type);
        this.paperType = paperType;
    }

    public PaperBukkitArgumentType(@NotNull Class<T> type, @NotNull PaperArgumentType<N, T> paperType) {
        super(type);
        this.paperType = paperType;
    }

    /**
     * Convenience factory for callers that want to skip the type
     * parameter dance.
     */
    public static <N, T> @NotNull PaperBukkitArgumentType<N, T> of(@NotNull Class<T> type, @NotNull PaperArgumentType<N, T> paperType) {
        return new PaperBukkitArgumentType<>(type, paperType);
    }

    /**
     * Counts how many whitespace-separated tokens of {@code joined} fall
     * within the first {@code charPos} characters. Conservative — leading
     * whitespace produces an empty leading token which is filtered out so
     * pure-whitespace prefixes don't artificially inflate the count.
     * Used to translate Brigadier's char-position cursor back into the
     * token-index space Imperat's {@link Cursor} operates in.
     */
    private static int countTokensConsumed(@NotNull String joined, int charPos) {
        if (charPos <= 0) {
            return 0;
        }
        String consumed = charPos >= joined.length() ? joined : joined.substring(0, charPos);
        consumed = consumed.trim();
        if (consumed.isEmpty()) {
            return 0;
        }
        return consumed.split("\\s+").length;
    }

    @Override
    public T parse(@NotNull CommandContext<BukkitCommandSource> context,
            @NotNull Argument<BukkitCommandSource> argument,
            @NotNull Cursor<BukkitCommandSource> cursor) throws CommandException {
        // Multi-token Paper types (positions, item-stack with components,
        // etc.) need their tokens joined for Brigadier's StringReader. The
        // tree allots tokens via {@link #getNumberOfParametersToConsume};
        // here we drain whatever the cursor was given.

        Cursor<BukkitCommandSource> delegate = cursor.snapshot();
        String joined = delegate.collectRemaining();

        try {
            StringReader reader = new StringReader(joined);
            N nativeValue = paperType.nativeType().parse(reader);

            // Brigadier's {@code reader.getCursor()} is a CHARACTER index
            // in {@code joined}; Imperat's {@link Cursor#position()} is a
            // TOKEN index. Writing the char index back via
            // {@code commitFromPosition} corrupts the token cursor and
            // makes downstream {@code slice(0, position)} blow up with
            // "out of bounds for size N" when the native parser consumed
            // multiple chars (longer than the cursor's token count).
            // Convert by counting whitespace-separated tokens up to the
            // char cursor, then advance the underlying cursor token by
            // token so the subsequent budget checks see the right count.
            int charPos = reader.getCursor();
            int tokensConsumed = countTokensConsumed(joined, charPos);
            for (int i = 0; i < tokensConsumed && cursor.hasNext(); i++) {
                cursor.next();
            }

            // Stack is held as Object on BukkitCommandSource so the source
            // class stays loadable on legacy classpaths. Modern backend
            // always populates it with a CommandSourceStack instance — cast
            // safe here since this argument type only runs on modern Paper.
            CommandSourceStack stack = (CommandSourceStack) context.source().stack();
            return paperType.resolver().apply(nativeValue, stack);
        } catch (CommandSyntaxException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
    }

    /** Native Paper Brigadier type — used by the registrar to wire client-side suggestions. */
    @Override
    public @NotNull com.mojang.brigadier.arguments.ArgumentType<N> nativeType() {
        return paperType.nativeType();
    }

    public @NotNull PaperArgumentType<N, T> paperType() {
        return paperType;
    }
}
