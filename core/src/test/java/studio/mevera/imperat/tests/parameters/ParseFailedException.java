package studio.mevera.imperat.tests.parameters;

import studio.mevera.imperat.exception.CommandException;

public final class ParseFailedException extends CommandException {
    public ParseFailedException(String message) {
        super(message);
    }
}
