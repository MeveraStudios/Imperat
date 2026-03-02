package studio.mevera.imperat.exception;

import org.jetbrains.annotations.Nullable;

/**
 * Thrown when a command source lacks the required permission to execute a command or usage.
 * <p>
 * Carries {@code %command%} and {@code %usage%} placeholders
 * which are used by the throwable resolver in {@code ImperatConfigImpl}.
 */
public final class PermissionDeniedException extends CommandException {

    private final String command;
    private final @Nullable String usage;

    /**
     * @param command the name of the command the source attempted to use
     * @param usage   the formatted usage string, or {@code null} if unavailable
     */
    public PermissionDeniedException(String command, @Nullable String usage) {
        super("You don't have permission to use '"
                      + command
                      + (usage != null && !usage.isBlank() ? " " + usage : "")
                      + "'");
        this.command = command;
        this.usage = usage;
    }

    public String getCommand() {
        return command;
    }

    public @Nullable String getUsage() {
        return usage;
    }
}
