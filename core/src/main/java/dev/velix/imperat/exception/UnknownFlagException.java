package dev.velix.imperat.exception;

public final class UnknownFlagException extends ParseException {
    
    public UnknownFlagException(String input) {
        super(input);
    }
}
