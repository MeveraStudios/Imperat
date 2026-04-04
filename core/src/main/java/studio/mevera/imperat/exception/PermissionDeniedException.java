package studio.mevera.imperat.exception;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.permissions.PermissionHolder;

/**
 * Thrown when a command source lacks the required permission to execute a command or usage.
 * <p>
 * Carries {@code %command%} and {@code %usage%} placeholders
 * which are used by the throwable resolver in {@code ImperatConfigImpl}.
 */
public final class PermissionDeniedException extends CommandException {

    private final String label;
    private final CommandPathway<? extends CommandSource> executingPathway;
    private final PermissionHolder permissionIssuer;

    public PermissionDeniedException(String label, CommandPathway<? extends CommandSource> executingPathway, PermissionHolder permissionIssuer) {
        super();
        this.label = label;
        this.executingPathway = executingPathway;
        this.permissionIssuer = permissionIssuer;
    }

    /**
     * @return the pathway that couldn't be executed due to lack of permissions
     */
    public CommandPathway<? extends CommandSource> getExecutingPathway() {
        return executingPathway;
    }

    /**
     * @return The label/alias provided in the input for the root command.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return The permission holding object, in which, the source didn't have the permission for to be used.
     * It can be either {@link Argument} or {@link CommandPathway} or even {@link Command} (including sub-commands)
     */
    public PermissionHolder getPermissionIssuer() {
        return permissionIssuer;
    }
}
