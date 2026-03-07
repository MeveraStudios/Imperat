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

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class CollectionArgument<S extends CommandSource, E, C extends Collection<E>> extends ArgumentType<S, C> {

    private final Supplier<C> collectionSupplier;
    private final ArgumentType<S, E> componentResolver;

    public CollectionArgument(TypeWrap<C> type, Supplier<C> collectionSupplier, ArgumentType<S, E> componentResolver) {
        super(type.getType());
        this.collectionSupplier = collectionSupplier;
        this.componentResolver = componentResolver;
    }

    @Override
    public @Nullable C parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor, @NotNull String correspondingInput) throws
            CommandException {
        C newCollection = collectionSupplier.get();

        String currentRaw = cursor.currentRaw().orElse(null);
        if (currentRaw == null) {
            return newCollection;
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
        newCollection.add(componentResolver.parse(context, Cursor.subStream(cursor, currentRaw), currentRaw));
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

            newCollection.add(componentResolver.parse(context, Cursor.subStream(cursor, raw), raw));
            consumed++;
        }

        return newCollection;
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
