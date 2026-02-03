package studio.mevera.imperat.command.parameters.validator;

import studio.mevera.imperat.exception.CommandException;

public class InvalidArgumentException extends CommandException {

    public InvalidArgumentException(String message) {
        super(message);
    }
}
