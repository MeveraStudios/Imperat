package studio.mevera.imperat.exception;

import org.jetbrains.annotations.Nullable;

/**
 * Thrown when a command's syntax does not match any valid usage pathway.
 * <p>
 * Carries {@code %invalid_usage%} and {@code %closest_usage%} placeholders
 * which are used by the throwable resolver in {@code ImperatConfigImpl}.
 */
public final class InvalidSyntaxException extends CommandException {

    private final String invalidUsage;
    private final @Nullable String closestUsage;

    /**
     * @param invalidUsage  what the user actually typed (e.g. "/give sword stone")
     * @param closestUsage  the closest valid usage hint, or {@code null} if unavailable
     */
    public InvalidSyntaxException(String invalidUsage, @Nullable String closestUsage) {
        super("Invalid command usage '" + invalidUsage + "'"
                      + (closestUsage != null && !closestUsage.isBlank()
                                 ? ", you probably meant '" + closestUsage + "'"
                                 : ""));
        this.invalidUsage = invalidUsage;
        this.closestUsage = closestUsage;
    }

    public String getInvalidUsage() {
        return invalidUsage;
    }

    public @Nullable String getClosestUsage() {
        return closestUsage;
    }
}
