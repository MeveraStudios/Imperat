package studio.mevera.imperat.exception;

import studio.mevera.imperat.annotations.base.element.MethodElement;

import java.lang.reflect.Type;

public class InvalidSourceException extends RuntimeException {

    public InvalidSourceException(MethodElement method, Type invalidType) {
        super("Method '" + method.getName() + "' has an invalid source type: " + invalidType.getTypeName());
    }

}
