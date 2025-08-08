package studio.mevera.imperat.exception;

import studio.mevera.imperat.VelocitySource;
import studio.mevera.imperat.context.Context;

public class UnknownPlayerException extends ImperatException {

    private final String name;

    public UnknownPlayerException(final String name, Context<VelocitySource> ctx) {
        super(ctx);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
