package studio.mevera.imperat.command.parameters.validator;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Argument;
import studio.mevera.imperat.exception.NumberOutOfRangeException;
import studio.mevera.imperat.util.Priority;

public final class RangeValidator<S extends Source> implements ArgValidator<S> {

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }

    @Override
    public void validate(Context<S> context, Argument<S> argument) throws InvalidArgumentException {
        Object value = argument.value();
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            var param = argument.parameter().asNumeric();
            var range = param.getRange();

            if(range != null && !range.matches(doubleValue)) {
                throw new NumberOutOfRangeException(argument.raw(), argument.parameter().asNumeric(), number, range);
            }

        }else {
            throw new InvalidArgumentException("Argument '" + argument.name() + "' is not a number.");
        }
    }
}
