package studio.mevera.imperat.context.internal;

import studio.mevera.imperat.context.Source;

import java.util.function.IntUnaryOperator;

public final class StreamPosition<S extends Source> {
    
    int maxParamLength, maxRawLength;
    int parameter, raw;

    StreamPosition(int maxParamLength, int maxRawLength, int parameter, int raw) {
        this.maxParamLength = maxParamLength;
        this.maxRawLength = maxRawLength;
        this.parameter = parameter;
        this.raw = raw;
    }

    StreamPosition(int maxParamLength, int maxRawLength) {
        this(maxParamLength, maxRawLength, 0, 0);
    }
    
    public int getRaw() {
        return raw;
    }
    
    public int getParameter() {
        return parameter;
    }
    
    void shift(ShiftTarget shift, IntUnaryOperator operator) {
        switch (shift) {
            case RAW_ONLY -> this.raw = operator.applyAsInt(raw);
            case PARAMETER_ONLY -> this.parameter = operator.applyAsInt(parameter);
            default -> {
                this.raw = operator.applyAsInt(raw);
                this.parameter = operator.applyAsInt(parameter);
            }
        }
    }

    void shift(ShiftTarget target, ShiftOperation operation) {
        shift(target, operation.operator);
    }
    
    void shiftRight(ShiftTarget target) {
        shift(target, ShiftOperation.RIGHT);
    }
    
    void shiftLeft(ShiftTarget target) {
        shift(target, ShiftOperation.LEFT);
    }

    boolean canContinue(
        ShiftTarget target
    ) {
        return target.canContinue(this);
    }

    boolean isLast(ShiftTarget shiftTarget, int maxParams, int maxRaws) {
        if (shiftTarget == ShiftTarget.PARAMETER_ONLY)
            return parameter == maxParams - 1;
        else if (shiftTarget == ShiftTarget.RAW_ONLY)
            return raw == maxRaws - 1;
        else
            return parameter == maxParams - 1 && raw == maxRaws - 1;
    }

    public boolean isLast(ShiftTarget shiftTarget) {
        return isLast(shiftTarget, maxParamLength, maxRawLength);
    }

    int maxRaws() {
        return maxRawLength;
    }

    int maxParameters() {
        return maxParamLength;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof StreamPosition<?> other)) return false;
        if (this.parameter != other.parameter) return false;
        return this.raw == other.raw;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + parameter;
        result = result * PRIME + raw;
        return result;
    }

    public StreamPosition<S> copy() {
        return new StreamPosition<>(maxParamLength, maxRawLength, parameter, raw);
    }
}
