package studio.mevera.imperat.command.parameters.validator;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.Priority;

public final class RangeValidator<S extends Source> implements ArgValidator<S> {

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }

    @Override
    public void validate(Context<S> context, ParsedArgument<S> parsedArgument) throws CommandException {
        Object value = parsedArgument.getArgumentParsedValue();
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            var param = parsedArgument.getOriginalArgument().asNumeric();
            var range = param.getRange();

            if (range != null && !range.matches(doubleValue)) {
                // Build range description
                final StringBuilder rangeBuilder = new StringBuilder();
                if (range.getMin() != Double.MIN_VALUE && range.getMax() != Double.MAX_VALUE) {
                    rangeBuilder.append("within ").append(range.getMin()).append('-').append(range.getMax());
                } else if (range.getMin() != Double.MIN_VALUE) {
                    rangeBuilder.append("at least '").append(range.getMin()).append("'");
                } else if (range.getMax() != Double.MAX_VALUE) {
                    rangeBuilder.append("at most '").append(range.getMax()).append("'");
                } else {
                    rangeBuilder.append("(Open range)");
                }

                throw new CommandException(ResponseKey.NUMBER_OUT_OF_RANGE)
                              .withPlaceholder("original_input", parsedArgument.getArgumentRawInput())
                              .withPlaceholder("value", String.valueOf(number))
                              .withPlaceholder("parameter", param.format())
                              .withPlaceholder("parameter_name", param.name())
                              .withPlaceholder("range", rangeBuilder.toString())
                              .withPlaceholder("range_min", String.valueOf(range.getMin()))
                              .withPlaceholder("range_max", String.valueOf(range.getMax()));
            }

        } else {
            throw new CommandException("Argument '" + parsedArgument.getArgumentName() + "' is not a number.");
        }
    }
}
