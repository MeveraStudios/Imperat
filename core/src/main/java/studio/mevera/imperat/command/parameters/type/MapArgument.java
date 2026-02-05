package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.parse.InvalidMapEntryFormatException;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Map;
import java.util.function.Supplier;

public class MapArgument<S extends Source, K, V, M extends Map<K, V>> extends ArgumentType<S, M> {

    private final static String ENTRY_SEPARATOR = ",";

    private final Supplier<M> mapInitializer;
    private final ArgumentType<S, K> keyResolver;
    private final ArgumentType<S, V> valueResolver;


    public MapArgument(
            TypeWrap<M> type,
            Supplier<M> mapInitializer,
            ArgumentType<S, K> keyResolver,
            ArgumentType<S, V> valueResolver
    ) {
        super(type.getType());
        this.mapInitializer = mapInitializer;
        this.keyResolver = keyResolver;
        this.valueResolver = valueResolver;
    }

    @Override
    public @Nullable M parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor, @NotNull String correspondingInput) throws
            CommandException {
        M newMap = mapInitializer.get();

        while (cursor.isCurrentRawInputAvailable()) {

            String raw = cursor.currentRaw().orElse(null);
            if (raw == null) {
                break;
            }

            if (!raw.contains(ENTRY_SEPARATOR)) {
                throw new InvalidMapEntryFormatException(raw, ENTRY_SEPARATOR, InvalidMapEntryFormatException.Reason.MISSING_SEPARATOR);
                //throw new SourceException("Invalid map entry '%s', entry doesn't contain '%s'", raw, ENTRY_SEPARATOR);
            }

            String[] split = raw.split(ENTRY_SEPARATOR);
            if (split.length != 2) {
                throw new InvalidMapEntryFormatException(raw, ENTRY_SEPARATOR, InvalidMapEntryFormatException.Reason.NOT_TWO_ELEMENTS);
                //throw new SourceException("Invalid map entry '%s', entry is not made of 2 elements", raw);
            }

            String keyRaw = split[0];
            String valueRaw = split[1];

            Cursor<S> keySubStream = Cursor.subStream(cursor, keyRaw);
            Cursor<S> valueSubStream = Cursor.subStream(cursor, keyRaw);

            K key = keyResolver.parse(context, keySubStream, keyRaw);
            V value = valueResolver.parse(context, valueSubStream, valueRaw);

            newMap.put(key, value);

            cursor.skipRaw();
        }
        return newMap;
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
