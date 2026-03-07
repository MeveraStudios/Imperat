package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.AvailableSince("1.9.8")
public class CombinedFlagsException extends CommandException {

    public CombinedFlagsException(String message) {
        super(message);
    }
}
