package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.context.Context;

@ApiStatus.AvailableSince("1.9.8")
public class ShortHandFlagException extends ImperatException {
    public ShortHandFlagException(String message, Context<?> context) {
        super(message, context);
    }
}
