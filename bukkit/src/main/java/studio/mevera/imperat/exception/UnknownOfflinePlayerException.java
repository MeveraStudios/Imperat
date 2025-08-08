package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public class UnknownOfflinePlayerException extends ImperatException {

    private final String name;

    public UnknownOfflinePlayerException(final String name, Context<?> ctx) {
        super(ctx);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
