package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public class OnlyConsoleAllowedException extends ImperatException {
    
    public OnlyConsoleAllowedException(Context<?> ctx) {
        super(ctx);
    }
}
