package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public class UnknownServerException extends ParseException {
    
    public UnknownServerException(String input, Context<?> ctx) {
        super(input, ctx);
    }
}
