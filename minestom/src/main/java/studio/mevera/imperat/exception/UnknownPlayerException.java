package studio.mevera.imperat.exception;

import studio.mevera.imperat.responses.MinestomResponseKey;

public class UnknownPlayerException extends ArgumentParseException {

    public UnknownPlayerException(final String name) {
        super(MinestomResponseKey.UNKNOWN_PLAYER, name);
    }

    public String getName() {
        return getInput();
    }
}
