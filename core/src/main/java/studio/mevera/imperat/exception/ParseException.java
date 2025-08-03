package studio.mevera.imperat.exception;

public abstract class ParseException extends ImperatException {

    protected final String input;

    public ParseException(String input) {
        super("Failed to parse input '" + input + "'");
        this.input = input;
    }

    public String getInput() {
        return input;
    }
}
