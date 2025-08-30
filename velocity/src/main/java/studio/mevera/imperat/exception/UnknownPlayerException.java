package studio.mevera.imperat.exception;

import studio.mevera.imperat.VelocitySource;
import studio.mevera.imperat.context.Context;

public class UnknownPlayerException extends ParseException {
    
    public UnknownPlayerException(final String name, Context<VelocitySource> ctx) {
        super(name, ctx);
    }

}
