package studio.mevera.imperat.exception;

public class UnknownUserException extends CommandException {

    private final String identifier;

    public UnknownUserException(String identifier) {
        super("Unknown user: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
