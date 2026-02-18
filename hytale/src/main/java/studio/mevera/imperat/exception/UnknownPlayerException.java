package studio.mevera.imperat.exception;

import studio.mevera.imperat.responses.HytaleResponseKey;

public class UnknownPlayerException extends CommandException {

    private final String name;

    public UnknownPlayerException(final String name) {
        super(HytaleResponseKey.UNKNOWN_PLAYER);
        this.name = name;
        withPlaceholder("player", name);
    }

    public String getName() {
        return name;
    }
}
