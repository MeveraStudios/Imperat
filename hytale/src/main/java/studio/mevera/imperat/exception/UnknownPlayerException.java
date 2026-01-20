package studio.mevera.imperat.exception;

import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.context.Context;

public class UnknownPlayerException extends ParseException {

    private final String name;

    public UnknownPlayerException(final String name, Context<HytaleSource> context) {
        super(name, context);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
