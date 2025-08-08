package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public final class UnknownFlagException extends ParseException {
    
    public UnknownFlagException(String input, Context<?> context) {
        super(input, context);
    }
}
