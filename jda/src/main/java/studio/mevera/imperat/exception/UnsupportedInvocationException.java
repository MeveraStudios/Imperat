package studio.mevera.imperat.exception;

/**
 * Thrown when a command needs a slash command but ran from a text message, or the other way round.
 */
public class UnsupportedInvocationException extends CommandException {

    public UnsupportedInvocationException(String message) {
        super(message);
    }
}
