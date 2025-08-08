package studio.mevera.imperat.exception;

import studio.mevera.imperat.MinestomSource;
import studio.mevera.imperat.context.Context;

public class UnknownPlayerException extends ImperatException {

    private final String name;

    public UnknownPlayerException(final String name, Context<MinestomSource> context) {
        super(context);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
