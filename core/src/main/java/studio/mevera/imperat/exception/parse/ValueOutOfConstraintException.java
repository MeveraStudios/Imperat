package studio.mevera.imperat.exception.parse;

import studio.mevera.imperat.command.parameters.validator.InvalidArgumentException;

import java.util.Set;

public class ValueOutOfConstraintException extends InvalidArgumentException {

    private final Set<String> allowedValues;
    private final String input;
    public ValueOutOfConstraintException(String input, Set<String> allowedValues) {
        super("Value '" + input + "' is not within the allowed values: " + allowedValues);
        this.input = input;
        this.allowedValues = allowedValues;
    }

    public String getInput() {
        return input;
    }

    public Set<String> getAllowedValues() {
        return allowedValues;
    }

}
