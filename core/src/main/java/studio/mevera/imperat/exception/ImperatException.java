package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.context.Context;

@ApiStatus.AvailableSince("1.0.0")
public class ImperatException extends Exception {
    
    private final Context<?> ctx;
    public ImperatException(String message, Throwable cause, Context<?> ctx) {
        super(message, cause);
        this.ctx = ctx;
    }
    
    public ImperatException(String message, Context<?> ctx) {
        super(message);
        this.ctx = ctx;
    }
    
    public ImperatException(Context<?> ctx) {
        super();
        this.ctx = ctx;
    }
    
    
    public Context<?> getCtx() {
        return ctx;
    }
}
