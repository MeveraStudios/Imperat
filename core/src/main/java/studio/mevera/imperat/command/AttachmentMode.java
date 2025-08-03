package studio.mevera.imperat.command;

import org.jetbrains.annotations.ApiStatus;

/**
 * Defines the modes of attachment for a subcommand within a {@link Command}.
 * This enum determines how a subcommand integrates with the usages of a command,
 * based on the specified attachment mode.
 */
@ApiStatus.AvailableSince("1.9.0")
public enum AttachmentMode {


    /**
     * Attaches the subcommand to an empty usage of the {@link Command}
     */
    EMPTY(false),

    /**
     * Attaches the subcommand to the default usage of a {@link Command},
     * which may be empty or contain optional arguments/parameters.
     */
    DEFAULT(true),

    /**
     * Attaches the sub command to the main {@link CommandUsage} of a {@link Command}
     * A main usage is a usage that contains required arguments/parameters.
     */
    MAIN(true),

    /**
     * Behaves exactly like {@link AttachmentMode#MAIN},
     * It's used internally to know when to prefer the global attachment mode over the per subcommand one.
     */
    UNSET(true),
    ;

    private final boolean requiresParameterInheritance;

    AttachmentMode(boolean requiresParameterInheritance) {
        this.requiresParameterInheritance = requiresParameterInheritance;
    }


    public boolean requiresParameterInheritance() {
        return requiresParameterInheritance;
    }
}
