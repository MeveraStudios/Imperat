package studio.mevera.imperat.exception.selector;

import studio.mevera.imperat.exception.ParseException;

public class UnknownSelectorFieldException extends ParseException {

    private final String fieldEntered;

    public UnknownSelectorFieldException(String fieldEntered, String input) {
        super(input);
        this.fieldEntered = fieldEntered;
    }

    public String getFieldEntered() {
        return fieldEntered;
    }
}
