package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.AvailableSince("1.0.0")
public class CommandException extends Exception {

    public CommandException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

    public CommandException(String message, Object... args) {
        super(String.format(message, args));
    }

    public CommandException() {
        super();
    }
}
