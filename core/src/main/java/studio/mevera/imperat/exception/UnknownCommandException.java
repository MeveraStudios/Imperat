package studio.mevera.imperat.exception;

import org.jetbrains.annotations.NotNull;

public class UnknownCommandException extends RuntimeException {

    private final String command;

    public UnknownCommandException(String command) {
        super("No command named '" + command + "' is registered");
        this.command = command;
    }

    @NotNull
    public String getCommand() {
        return command;
    }
}

