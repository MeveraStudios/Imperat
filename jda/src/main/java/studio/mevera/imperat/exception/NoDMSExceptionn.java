package studio.mevera.imperat.exception;

import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.context.Context;

/**
 * Exception thrown when a member is requested in a non-guild (dms) context.
 */
public class NoDMSExceptionn extends ImperatException {

    public NoDMSExceptionn(Context<JdaSource> context) {
        super("Command is only available in the dedicated discord server, not dms", context);
    }

}
