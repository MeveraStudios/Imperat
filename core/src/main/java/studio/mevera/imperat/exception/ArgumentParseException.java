package studio.mevera.imperat.exception;

import studio.mevera.imperat.responses.ResponseKey;

/**
 * Thrown when a raw argument string cannot be parsed into the expected type.
 * <p>
 * Automatically registers {@code %input%} as a placeholder so every response
 * message can reference the offending input without extra boilerplate at the
 * throw site.
 * <p>
 * Subclasses may call {@link #withPlaceholder} to add further context-specific
 * placeholders on top of the base {@code %input%}.
 */
public class ArgumentParseException extends ResponseException {

    private final String input;

    public ArgumentParseException(ResponseKey responseKey, String input) {
        super(responseKey);
        this.input = input;
        withPlaceholder("input", input);
    }

    public String getInput() {
        return input;
    }
}

