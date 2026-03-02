package studio.mevera.imperat.exception;

import studio.mevera.imperat.responses.JdaResponseKey;

public class JdaArgumentParseException extends ArgumentParseException {

    public JdaArgumentParseException(JdaResponseKey responseKey, String input) {
        super(responseKey, input);
    }
}
