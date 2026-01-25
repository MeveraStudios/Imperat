package studio.mevera.imperat.exception.parse;

import studio.mevera.imperat.exception.ParseException;

public final class InvalidBooleanException extends ParseException {

    public InvalidBooleanException(String input) {
        super(input);
    }
}
