package studio.mevera.imperat.exception;

import studio.mevera.imperat.command.parameters.NumericParameter;
import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.command.parameters.validator.InvalidArgumentException;

public class NumberOutOfRangeException extends InvalidArgumentException {

    private final NumericParameter<?> parameter;
    private final Number value;
    private final NumericRange range;

    public NumberOutOfRangeException(
            final String originalInput,
            final NumericParameter<?> parameter,
            final Number value,
            final NumericRange range
    ) {
        super(originalInput);
        this.parameter = parameter;
        this.value = value;
        this.range = range;
    }

    public Number getValue() {
        return value;
    }

    public NumericRange getRange() {
        return range;
    }

    public NumericParameter<?> getParameter() {
        return parameter;
    }

}
