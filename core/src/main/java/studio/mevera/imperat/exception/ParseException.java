package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

public abstract class ParseException extends ImperatException {

    protected final String input;

    public ParseException(String input, Context<?> ctx) {
        super("Failed to parse input '" + input + "'", ctx);
        this.input = input;
    }

    public String getInput() {
        return input;
    }
}
