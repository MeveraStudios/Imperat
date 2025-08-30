package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public class OnlyPlayerAllowedException extends ImperatException {
    
    public OnlyPlayerAllowedException(Context<?> ctx) {
        super(ctx);
    }
}
