package studio.mevera.imperat.exception;

import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;

public class UnknownWorldException extends ImperatException {

    private final String name;

    public UnknownWorldException(final String name, Context<BukkitSource> ctx) {
        super(ctx);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
