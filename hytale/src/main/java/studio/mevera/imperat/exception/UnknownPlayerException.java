package studio.mevera.imperat.exception;

import studio.mevera.imperat.responses.HytaleResponseKey;

public class UnknownPlayerException extends ArgumentParseException {

    public UnknownPlayerException(final String name) {
        super(HytaleResponseKey.UNKNOWN_PLAYER, name);
    }

    public String getName() {
        return getInput();
    }
}
