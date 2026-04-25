package studio.mevera.imperat.exception;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;

/**
 * Thrown when a command's syntax does not match any valid usage pathway.
 * <p>
 * Carries {@code %invalid_usage%} and {@code %closest_usage%} placeholders
 * which are used by the throwable resolver in {@code ImperatConfigImpl}.
 */
public final class InvalidSyntaxException extends CommandException {

    private final String invalidUsage;
    private final @Nullable CommandPathway<? extends CommandSource> closestUsage;

    /**
     * @param invalidUsage  what the user actually typed (e.g. "/give sword stone")
     * @param closestUsage  the closest valid usage hint, or {@code null} if unavailable
     */
    public <S extends CommandSource> InvalidSyntaxException(String invalidUsage, @Nullable CommandPathway<S> closestUsage) {
        super("Invalid command usage '%s'", invalidUsage);
        this.invalidUsage = invalidUsage;
        this.closestUsage = closestUsage;
    }

    public String getInvalidUsage() {
        return invalidUsage;
    }

    public @Nullable CommandPathway<? extends CommandSource> getClosestUsage() {
        return closestUsage;
    }
}
