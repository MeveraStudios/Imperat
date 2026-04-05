package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeWrap;
import java.util.Map;
import java.util.function.Supplier;

public class MapArgument<S extends CommandSource, K, V, M extends Map<K, V>> extends ArgumentType<S, M> {
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
    public M parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws CommandException {
        // Parse a single map entry from a string (e.g., "key,value")
        M newMap = mapInitializer.get();
        parseAndAddEntry(context, argument, input, newMap);
        return newMap;
    }

    @Override
    public M parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws CommandException {
        M newMap = mapInitializer.get();
        String currentRaw = cursor.currentRaw().orElse(null);
        if (currentRaw == null) {
            return newMap; // empty map
        }
        Argument<S> currentParam = cursor.currentParameterIfPresent();
        int greedyLimit = currentParam != null ? currentParam.greedyLimit() : -1;
        Argument<S> nextParam = GreedyLimitHelper.findNextNonFlagParam(cursor);
        boolean nextParamCanDiscriminate = nextParam != null && !(nextParam.type() instanceof StringArgument<?>);
        int effectiveLimit = GreedyLimitHelper.computeEffectiveLimit(greedyLimit, nextParam, nextParamCanDiscriminate, cursor);
        int consumed = 0;
        // Consume the first raw
        parseAndAddEntry(context, currentParam, currentRaw, newMap);
        consumed++;
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
                break;
            }
            // Stop: next param has a discriminating type and peeked matches it
            if (nextParamCanDiscriminate) {
                // Use parse-based validation instead of matchesInput
                try {
                    assert currentParam != null;
                    nextParam.type().parse(context, currentParam, peeked);
                    break;
                } catch (Exception ignored) {
                    // Not a match, continue
                }
            }
            cursor.skipRaw();
            String raw = cursor.currentRaw().orElse(null);
            if (raw == null) {
                break;
            }
            parseAndAddEntry(context, currentParam, raw, newMap);
            consumed++;
        }
        return newMap;
    }

    private void parseAndAddEntry(CommandContext<S> context, Argument<S> argument, String raw, M map) throws CommandException {
        if (!raw.contains(ENTRY_SEPARATOR)) {
            throw new ArgumentParseException(ResponseKey.INVALID_MAP_ENTRY_FORMAT, raw)
                          .withPlaceholder("extra_msg", ", entry doesn't contain '" + ENTRY_SEPARATOR + "'");
        }

        String[] split = raw.split(ENTRY_SEPARATOR, 2);
        if (split.length != 2) {
            throw new ArgumentParseException(ResponseKey.INVALID_MAP_ENTRY_FORMAT, raw)
                          .withPlaceholder("extra_msg", ", entry is not made of 2 elements");
        }

        String keyRaw = split[0];
        String valueRaw = split[1];

        K key = keyResolver.parse(context, argument, keyRaw);
        V value = valueResolver.parse(context, argument, valueRaw);
        map.put(key, value);
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
