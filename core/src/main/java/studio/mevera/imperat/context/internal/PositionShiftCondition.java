package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
interface PositionShiftCondition {
    boolean canContinue(@NotNull StreamPosition<?> streamPosition);
}