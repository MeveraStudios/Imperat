package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Map;
import java.util.Set;
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

        String currentRaw = cursor.currentRaw().orElse(null);
        if (currentRaw == null) {
            return newMap;
        }

        Argument<S> currentParam = cursor.currentParameterIfPresent();
        int greedyLimit = currentParam != null ? currentParam.greedyLimit() : -1;

        Argument<S> nextParam = GreedyLimitHelper.findNextNonFlagParam(cursor);
        boolean nextParamCanDiscriminate = nextParam != null
                                                   && !(nextParam.type() instanceof StringArgument<?>);

        int effectiveLimit = GreedyLimitHelper.computeEffectiveLimit(
                greedyLimit, nextParam, nextParamCanDiscriminate, cursor
        );

        // Consume the first raw
        parseAndAddEntry(context, cursor, currentRaw, newMap);
        int consumed = 1;

        // Consume subsequent raws
        while (cursor.hasNextRaw()) {
            if (effectiveLimit > 0 && consumed >= effectiveLimit) {
                break;
            }

            String peeked = cursor.peekRawIfPresent();
            if (peeked == null) {
                break;
            }

            // Stop: next raw is a flag
            if (Patterns.isInputFlag(peeked)) {
                Set<FlagArgument<S>> extracted = context.getDetectedPathway()
                                                         .getFlagExtractor().extract(peeked);
                if (!extracted.isEmpty()) {
                    cursor.skipRaw();
                    cursor.skipRaw();
                    if (extracted.stream().noneMatch(FlagArgument::isSwitch)) {
                        cursor.skipRaw();
                    }
                    continue;
                }
            }

            // Stop: next param has a discriminating type and peeked matches it
            if (nextParamCanDiscriminate) {
                int peekRawPos = cursor.currentRawPosition() + 1;
                if (nextParam.type().matchesInput(peekRawPos, context, nextParam)) {
                    break;
                }
            }

            cursor.skipRaw();
            String raw = cursor.currentRaw().orElse(null);
            if (raw == null) {
                break;
            }

            parseAndAddEntry(context, cursor, raw, newMap);
            consumed++;
        }

        return newMap;
    }

    private void parseAndAddEntry(ExecutionContext<S> context, Cursor<S> cursor, String raw, M map) throws CommandException {
        if (!raw.contains(ENTRY_SEPARATOR)) {
            throw new ArgumentParseException(ResponseKey.INVALID_MAP_ENTRY_FORMAT, raw)
                          .withPlaceholder("extra_msg", ", entry doesn't contain '" + ENTRY_SEPARATOR + "'");
        }

        String[] split = raw.split(ENTRY_SEPARATOR);
        if (split.length != 2) {
            throw new ArgumentParseException(ResponseKey.INVALID_MAP_ENTRY_FORMAT, raw)
                          .withPlaceholder("extra_msg", ", entry is not made of 2 elements");
        }

        String keyRaw = split[0];
        String valueRaw = split[1];

        Cursor<S> keySubStream = Cursor.subStream(cursor, keyRaw);
        Cursor<S> valueSubStream = Cursor.subStream(cursor, keyRaw);

        K key = keyResolver.parse(context, keySubStream, keyRaw);
        V value = valueResolver.parse(context, valueSubStream, valueRaw);

        map.put(key, value);
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
