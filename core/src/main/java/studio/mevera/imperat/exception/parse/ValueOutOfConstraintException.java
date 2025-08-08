package studio.mevera.imperat.exception.parse;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ParseException;

import java.util.Set;

public class ValueOutOfConstraintException extends ParseException {

    private final Set<String> allowedValues;

    public ValueOutOfConstraintException(String input, Set<String> allowedValues, Context<?> ctx) {
        super(input, ctx);
        this.allowedValues = allowedValues;
    }

    public Set<String> getAllowedValues() {
        return allowedValues;
    }

}
