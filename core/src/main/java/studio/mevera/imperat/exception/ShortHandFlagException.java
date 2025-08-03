package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.AvailableSince("1.9.8")
public class ShortHandFlagException extends ImperatException {
    public ShortHandFlagException(String message) {
        super(message);
    }
}
