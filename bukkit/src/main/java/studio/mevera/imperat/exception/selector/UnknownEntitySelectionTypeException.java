package studio.mevera.imperat.exception.selector;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ParseException;

public class UnknownEntitySelectionTypeException extends ParseException {

    public UnknownEntitySelectionTypeException(String input, Context<?> ctx) {
        super(input, ctx);
    }
}
