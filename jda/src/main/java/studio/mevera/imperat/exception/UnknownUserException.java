package studio.mevera.imperat.exception;

import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.context.Context;

public class UnknownUserException extends ImperatException {
    private final String identifier;

    public UnknownUserException(String identifier, Context<JdaSource> context) {
        super("Unknown user: " + identifier, context);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
