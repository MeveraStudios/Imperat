package studio.mevera.imperat.exception;


import studio.mevera.imperat.responses.MinestomResponseKey;

public class UnknownPlayerException extends CommandException {

    private final String name;

    public UnknownPlayerException(final String name) {
        super(MinestomResponseKey.UNKNOWN_PLAYER);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
