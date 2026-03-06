package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.exception.CommandException;

/**
 * Custom exception type used to test command-specific {@code @ExceptionHandler} methods.
 */
public class TestCommandException extends CommandException {

    public TestCommandException(String message) {
        super(message);
    }
}

