package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.internal.Cursor;

/**
 * Shared utility for greedy-limit calculations used by
 * {@link ArrayArgument}, {@link CollectionArgument}, and {@link MapArgument}.
 * <p>
 * Mirrors the logic already present in {@link StringArgument} so that all
 * greedy argument types behave consistently when {@code @Greedy(limit = N)}
 * is specified.
 */
final class GreedyLimitHelper {

    private GreedyLimitHelper() {
        // utility
    }

    /**
     * Finds the next non-flag parameter after the current cursor position,
     * or {@code null} if there is none.
     */
    static <S extends CommandSource> @Nullable Argument<S> findNextNonFlagParam(Cursor<S> cursor) {
        int currentPos = cursor.currentParameterPosition();
        for (int i = currentPos + 1; i < cursor.parametersLength(); i++) {
            Argument<S> param = cursor.getParametersList().get(i);
            if (!param.isFlag()) {
                return param;
            }
        }
        return null;
    }

    /**
     * Computes the effective token limit for a greedy argument, taking into
     * account the annotation limit and tokens that must be reserved for
     * trailing required parameters.
     *
     * @param greedyLimit            the raw limit from the annotation ({@code -1} = unlimited)
     * @param nextParam              the next non-flag parameter (may be {@code null})
     * @param nextParamCanDiscriminate whether the next param's type can distinguish tokens
     * @param cursor                 the current cursor
     * @return the effective limit ({@code -1} for unlimited, positive otherwise)
     */
    static <S extends CommandSource> int computeEffectiveLimit(
            int greedyLimit,
            @Nullable Argument<S> nextParam,
            boolean nextParamCanDiscriminate,
            Cursor<S> cursor
    ) {
        if (nextParam != null && !nextParamCanDiscriminate) {
            // Next param is also String-typed — reserve tokens for trailing required params
            int reserveForTrailing = countRemainingRequiredParams(cursor);
            int rawsLeft = cursor.rawsLength() - cursor.currentRawPosition();
            int maxByReserve = rawsLeft - reserveForTrailing;
            int effectiveLimit = greedyLimit > 0 ? Math.min(greedyLimit, maxByReserve) : maxByReserve;
            return Math.max(effectiveLimit, 1);
        }
        // Next param can discriminate OR there is no next param — use annotation limit
        return greedyLimit;
    }

    /**
     * Counts required non-flag parameters remaining AFTER the current parameter position.
     */
    private static <S extends CommandSource> int countRemainingRequiredParams(Cursor<S> cursor) {
        int count = 0;
        int currentPos = cursor.currentParameterPosition();
        for (int i = currentPos + 1; i < cursor.parametersLength(); i++) {
            Argument<S> param = cursor.getParametersList().get(i);
            if (param.isRequired() && !param.isFlag()) {
                count++;
            }
        }
        return count;
    }
}

