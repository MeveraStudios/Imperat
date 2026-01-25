package studio.mevera.imperat.exception.parse;

import studio.mevera.imperat.exception.ParseException;

public class InvalidMapEntryFormatException extends ParseException {

    private final String requiredSeparator;
    private final Reason reason;

    public InvalidMapEntryFormatException(String input, String requiredSeparator, Reason reason) {
        super(input);
        this.requiredSeparator = requiredSeparator;
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public String getRequiredSeparator() {
        return requiredSeparator;
    }

    public enum Reason {

        MISSING_SEPARATOR,

        NOT_TWO_ELEMENTS;

    }

}
