package studio.mevera.imperat.exception.selector;

import studio.mevera.imperat.exception.ParseException;

public class UnknownEntitySelectionTypeException extends ParseException {

    public UnknownEntitySelectionTypeException(String input) {
        super(input);
    }
}
