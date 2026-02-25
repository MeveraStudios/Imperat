package studio.mevera.imperat.exception;

/**
 * Exception thrown when a member is requested in a non-guild (DMs) context.
 */
public class NoDMSException extends CommandException {

    public NoDMSException() {
        super("RootCommand is only available in the dedicated discord server, not DMs");
    }

}
