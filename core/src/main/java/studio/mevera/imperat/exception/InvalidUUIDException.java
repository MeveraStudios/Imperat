package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public class InvalidUUIDException extends ParseException {
    
    public InvalidUUIDException(final String raw, Context<?> ctx) {
        super(raw, ctx);
    }
    
}
