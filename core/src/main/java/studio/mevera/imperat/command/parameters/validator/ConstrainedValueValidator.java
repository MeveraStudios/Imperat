package studio.mevera.imperat.command.parameters.validator;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;
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
    public void validate(CommandContext<S> context, ParsedArgument<S> parsedArgument) throws CommandException {
        String input = parsedArgument.getArgumentRawInput();
        if (!contains(input, allowedValues, caseSensitive)) {
            throw new CommandException(ResponseKey.VALUE_OUT_OF_CONSTRAINT)
                          .withPlaceholder("input", input)
                          .withPlaceholder("allowed_values", String.join(",", allowedValues));
        }
    }
}
