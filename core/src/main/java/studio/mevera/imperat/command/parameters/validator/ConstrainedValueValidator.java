package studio.mevera.imperat.command.parameters.validator;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Argument;
import studio.mevera.imperat.exception.parse.ValueOutOfConstraintException;
import studio.mevera.imperat.util.Priority;

import java.util.Set;

public final class ConstrainedValueValidator<S extends Source> implements ArgValidator<S> {

    private final Set<String> allowedValues;
    private final boolean caseSensitive;

    public ConstrainedValueValidator(Set<String> allowedValues, boolean caseSensitive) {
        this.allowedValues = allowedValues;
        this.caseSensitive = caseSensitive;
    }

    private static boolean contains(String input, Set<String> allowedValues, boolean caseSensitive) {
        if (caseSensitive) {
            return allowedValues.contains(input);
        }

        for (String value : allowedValues) {
            if (input.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.LOW;
    }

    @Override
    public void validate(Context<S> context, Argument<S> argument) throws InvalidArgumentException {
        String input = argument.raw();
        if (!contains(input, allowedValues, caseSensitive)) {
            throw new ValueOutOfConstraintException(input, allowedValues);
        }
    }
}
