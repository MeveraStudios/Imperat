package studio.mevera.imperat.tests.parameters;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ImperatException;

public class NotPlayerException extends ImperatException {
    public NotPlayerException(Context<?> ctx) {
        super(ctx);
    }
}
