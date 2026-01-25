package studio.mevera.imperat.exception;

public class UnknownMemberException extends CommandException {
    private final String identifier;

    public UnknownMemberException(String identifier) {
        super("Unknown member: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
