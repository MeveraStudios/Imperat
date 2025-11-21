package studio.mevera.imperat.exception;

import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ImperatException;

public class UnknownMemberException extends ImperatException {
    private final String identifier;

    public UnknownMemberException(String identifier, Context<JdaSource> context) {
        super("Unknown member: " + identifier, context);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
