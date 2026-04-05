package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Collection;
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
    public C parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws CommandException {
        C newCollection = collectionSupplier.get();
        String currentRaw = cursor.currentRaw().orElse(null);
        if (currentRaw == null) {
            return newCollection; // empty collection
        }
        Argument<S> currentParam = cursor.currentParameterIfPresent();
        int greedyLimit = currentParam != null ? currentParam.greedyLimit() : -1;
        Argument<S> nextParam = GreedyLimitHelper.findNextNonFlagParam(cursor);
        boolean nextParamCanDiscriminate = nextParam != null && !(nextParam.type() instanceof StringArgument<?>);
        int effectiveLimit = GreedyLimitHelper.computeEffectiveLimit(greedyLimit, nextParam, nextParamCanDiscriminate, cursor);
        int consumed = 0;
        // Consume the first raw
        assert currentParam != null;
        E firstValue = componentResolver.parse(context, currentParam, currentRaw);
        newCollection.add(firstValue);
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
                int peekRawPos = cursor.currentRawPosition() + 1;
                String peekedInput = context.arguments().getOr(peekRawPos, null);
                if (peekedInput != null) {
                    try {
                        nextParam.type().parse(context, currentParam, peekedInput);
                        break;
                    } catch (Exception ignored) {
                        // Not a match, continue
                    }
                }
            }
            cursor.skipRaw();
            String raw = cursor.currentRaw().orElse(null);
            if (raw == null) {
                break;
            }
            E value = componentResolver.parse(context, currentParam, raw);
            newCollection.add(value);
            consumed++;
        }
        return newCollection;
    }

    @Override
    public C parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws CommandException {
        // For single-string parsing, just parse one component and return a collection with it
        String[] raws = input.split(" ");
        C newCollection = collectionSupplier.get();
        // Consume the first raw
        E firstValue = componentResolver.parse(context, argument, input);
        newCollection.add(firstValue);
        // Consume subsequent raws
        for (String raw : raws) {
            E value = componentResolver.parse(context, argument, raw);
            newCollection.add(value);
        }

        return newCollection;
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return true;
    }
}
