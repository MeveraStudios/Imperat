package studio.mevera.imperat.exception.parse;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ParseException;

public class UnknownSubCommandException extends ParseException {

    public UnknownSubCommandException(String input, Context<?> ctx) {
        super(input, ctx);
    }
}
