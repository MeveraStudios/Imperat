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
public class PaperBukkitArgumentType<N, T> extends ArgumentType<BukkitCommandSource, T> {

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

            cursor.commitFromPosition(reader.getCursor());

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
    public @NotNull com.mojang.brigadier.arguments.ArgumentType<N> nativeType() {
        return paperType.nativeType();
    }

    public @NotNull PaperArgumentType<N, T> paperType() {
        return paperType;
    }
}
