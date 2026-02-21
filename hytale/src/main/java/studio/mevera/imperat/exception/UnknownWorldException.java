package studio.mevera.imperat.exception;

import studio.mevera.imperat.responses.HytaleResponseKey;

public class UnknownWorldException extends CommandException {

    private final String name;

    public UnknownWorldException(final String name) {
        super(HytaleResponseKey.UNKNOWN_WORLD);
        this.name = name;
        withPlaceholder("input", name);
    }

    public String getName() {
        return name;
    }

}