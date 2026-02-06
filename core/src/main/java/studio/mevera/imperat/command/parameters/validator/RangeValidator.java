package studio.mevera.imperat.command.parameters.validator;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.exception.NumberOutOfRangeException;
import studio.mevera.imperat.util.Priority;

public final class RangeValidator<S extends Source> implements ArgValidator<S> {

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }

    @Override
    public void validate(Context<S> context, ParsedArgument<S> parsedArgument) throws InvalidArgumentException {
        Object value = parsedArgument.getArgumentParsedValue();
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            var param = parsedArgument.getOriginalArgument().asNumeric();
            var range = param.getRange();

            if (range != null && !range.matches(doubleValue)) {
                throw new NumberOutOfRangeException(parsedArgument.getArgumentRawInput(), parsedArgument.getOriginalArgument().asNumeric(), number, range);
            }

        } else {
            throw new InvalidArgumentException("Argument '" + parsedArgument.getArgumentName() + "' is not a number.");
        }
    }
}
