package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public final class NoHelpPageException extends ImperatException {
    public NoHelpPageException(Context<?> ctx) {
        super(ctx);
    }
}
