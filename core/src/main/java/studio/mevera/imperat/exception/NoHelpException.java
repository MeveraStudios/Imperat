package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public final class NoHelpException extends ImperatException {
    public NoHelpException(Context<?> ctx) {
        super(ctx);
    }
}
