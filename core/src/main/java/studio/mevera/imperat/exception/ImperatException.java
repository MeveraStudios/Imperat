package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.AvailableSince("1.0.0")
public class ImperatException extends Exception {
    
    public ImperatException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ImperatException(String message) {
        super(message);
    }
    
    public ImperatException() {
        super();
    }
    
}
