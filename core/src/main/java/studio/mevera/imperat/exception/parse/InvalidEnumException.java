package studio.mevera.imperat.exception.parse;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ParseException;

public class InvalidEnumException extends ParseException {

    private final Class<? extends Enum> enumType;
    public InvalidEnumException(String input, Class<? extends Enum> enumType, Context<?> ctx) {
        super(input, ctx);
        this.enumType = enumType;
    }

    public Class<? extends Enum> getEnumType() {
        return enumType;
    }

}
