package studio.mevera.imperat.exception;

import studio.mevera.imperat.responses.JdaResponseKey;

public class JdaArgumentParseException extends CommandException {

    private final String input;

    public JdaArgumentParseException(JdaResponseKey responseKey, String input) {
        super(responseKey);
        this.input = input;
        withPlaceholder("input", input);
    }

    public String getInput() {
        return input;
    }

}
