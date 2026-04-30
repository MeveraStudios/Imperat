package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;

/**
 * Thrown when the same flag (by canonical name) is supplied more than
 * once in a single command invocation. Imperat models flags as
 * single-occurrence — repeating {@code --foo bar --foo baz} is treated
 * as a user error, NOT silently overwriting the earlier value.
 *
 * @since 4.0.0
 */
@ApiStatus.AvailableSince("4.0.0")
public final class DuplicateFlagException extends CommandException {

    private final String flagName;

    public DuplicateFlagException(String flagName) {
        super("Flag '" + flagName + "' was supplied more than once.");
        this.flagName = flagName;
    }

    public String getFlagName() {
        return flagName;
    }
}
