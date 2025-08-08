package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

import java.lang.reflect.Type;

public class InvalidSourceException extends ImperatException {

    private final Type targetType;

    public InvalidSourceException(Type targetType, Context<?> ctx) {
        super(ctx);
        this.targetType = targetType;
    }

    public Type getTargetType() {
        return targetType;
    }

}
