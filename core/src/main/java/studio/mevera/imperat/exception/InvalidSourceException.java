package studio.mevera.imperat.exception;

import java.lang.reflect.Type;

public class InvalidSourceException extends ImperatException {

    private final Type targetType;

    public InvalidSourceException(Type targetType) {
        super();
        this.targetType = targetType;
    }

    public Type getTargetType() {
        return targetType;
    }

}
