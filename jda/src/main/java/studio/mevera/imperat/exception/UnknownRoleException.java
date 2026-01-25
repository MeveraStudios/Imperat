package studio.mevera.imperat.exception;


public class UnknownRoleException extends CommandException {
    private final String identifier;

    public UnknownRoleException(String identifier) {
        super("Unknown role: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
