package dev.velix.imperat.command.parameters.type;

import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.exception.parse.InvalidMapEntryFormatException;
import dev.velix.imperat.util.TypeWrap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class ParameterMap<S extends Source, K, V, M extends Map<K, V>> extends BaseParameterType<S, M> {
    private final static String ENTRY_SEPARATOR = ",";

    private final Supplier<M> mapInitializer;
    private final ParameterType<S, K> keyResolver;
    private final ParameterType<S, V> valueResolver;


    public ParameterMap(
            TypeWrap<M> type,
            Supplier<M> mapInitializer,
            ParameterType<S, K> keyResolver,
            ParameterType<S, V> valueResolver
    ) {
        super(type.getType());
        this.mapInitializer = mapInitializer;
        this.keyResolver = keyResolver;
        this.valueResolver = valueResolver;
    }

    @Override
    public @Nullable M resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> commandInputStream, @NotNull String input) throws ImperatException {
        M newMap = mapInitializer.get();

        while (commandInputStream.hasNextRaw()) {

            String raw = commandInputStream.currentRaw().orElse(null);
            if(raw == null) break;

            if(!raw.contains(ENTRY_SEPARATOR)) {
                throw new InvalidMapEntryFormatException(raw, ENTRY_SEPARATOR, InvalidMapEntryFormatException.Reason.MISSING_SEPARATOR);
                //throw new SourceException("Invalid map entry '%s', entry doesn't contain '%s'", raw, ENTRY_SEPARATOR);
            }

            String[] split = raw.split(ENTRY_SEPARATOR);
            if(split.length != 2) {
                throw new InvalidMapEntryFormatException(raw,ENTRY_SEPARATOR, InvalidMapEntryFormatException.Reason.NOT_TWO_ELEMENTS);
                //throw new SourceException("Invalid map entry '%s', entry is not made of 2 elements", raw);
            }

            String keyRaw = split[0];
            String valueRaw = split[1];

            CommandInputStream<S> keySubStream = CommandInputStream.subStream(commandInputStream, keyRaw);
            CommandInputStream<S> valueSubStream = CommandInputStream.subStream(commandInputStream, keyRaw);

            K key = keyResolver.resolve(context, keySubStream, keyRaw);
            V value = valueResolver.resolve(context, valueSubStream, valueRaw);

            newMap.put(key, value);

            commandInputStream.skipRaw();
        }
        return newMap;
    }

}
