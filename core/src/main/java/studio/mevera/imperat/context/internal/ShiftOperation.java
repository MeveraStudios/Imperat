package studio.mevera.imperat.context.internal;

import java.util.function.IntUnaryOperator;

enum ShiftOperation {
    RIGHT(value -> value + 1),

    LEFT(value -> value - 1);

    final IntUnaryOperator operator;

    ShiftOperation(IntUnaryOperator operator) {
        this.operator = operator;
    }

}