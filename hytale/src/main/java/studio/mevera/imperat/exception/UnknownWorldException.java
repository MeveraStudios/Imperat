package studio.mevera.imperat.exception;

import studio.mevera.imperat.responses.HytaleResponseKey;

public class UnknownWorldException extends ArgumentParseException {

    public UnknownWorldException(final String name) {
        super(HytaleResponseKey.UNKNOWN_WORLD, name);
    }

    public String getName() {
        return getInput();
    }

}