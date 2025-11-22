package studio.mevera.imperat.exception;

import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.context.Context;

public class UnknownRoleException extends ImperatException {
    private final String identifier;

    public UnknownRoleException(String identifier, Context<JdaSource> context) {
        super("Unknown role: " + identifier, context);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
