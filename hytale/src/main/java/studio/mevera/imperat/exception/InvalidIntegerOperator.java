package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public class InvalidIntegerOperator extends ParseException {

    public InvalidIntegerOperator(String input, Context<?> ctx) {
        super(input, ctx);
    }
}
