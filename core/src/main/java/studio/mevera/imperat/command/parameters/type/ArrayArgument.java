package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.TypeWrap;

import java.util.function.Function;

public abstract class ArrayArgument<S extends Source, E> extends ArgumentType<S, E[]> {

    private final Function<Integer, Object[]> initializer;
    private final ArgumentType<S, E> componentType;

    public ArrayArgument(TypeWrap<E[]> type, Function<Integer, Object[]> initializer, ArgumentType<S, E> componentType) {
        super(type.getType());
        this.initializer = initializer;
        this.componentType = componentType;
    }

    @Override @SuppressWarnings("unchecked")
    public E @Nullable [] parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor, @NotNull String correspondingInput) throws
            CommandException {

        String currentRaw = cursor.currentRaw().orElse(null);
        if (currentRaw == null) {
            return null;
        }

        int arrayLength = cursor.rawsLength() - cursor.currentRawPosition();

        E[] array = (E[]) initializer.apply(arrayLength);

        int i = 0;
        while (cursor.isCurrentRawInputAvailable()) {

            String raw = cursor.currentRaw().orElse(null);
            if (raw == null) {
                break;
            }

            array[i] = componentType.parse(context, Cursor.subStream(cursor, raw), raw);

            cursor.skipRaw();
            i++;
        }

        return array;
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
