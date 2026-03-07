package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class ArrayArgument<S extends CommandSource, E> extends ArgumentType<S, E[]> {

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

        Argument<S> currentParam = cursor.currentParameterIfPresent();
        int greedyLimit = currentParam != null ? currentParam.greedyLimit() : -1;

        // Find the next non-flag parameter (for type-discriminated yielding)
        Argument<S> nextParam = GreedyLimitHelper.findNextNonFlagParam(cursor);
        boolean nextParamCanDiscriminate = nextParam != null
                                                   && !(nextParam.type() instanceof StringArgument<?>);

        int effectiveLimit = GreedyLimitHelper.computeEffectiveLimit(
                greedyLimit, nextParam, nextParamCanDiscriminate, cursor
        );

        List<E> elements = new ArrayList<>();
        int consumed = 0;

        // Consume the first raw (cursor currently points at it)
        elements.add(componentType.parse(context, Cursor.subStream(cursor, currentRaw), currentRaw));
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

            elements.add(componentType.parse(context, Cursor.subStream(cursor, raw), raw));
            consumed++;
        }

        E[] array = (E[]) initializer.apply(elements.size());
        return elements.toArray(array);
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
