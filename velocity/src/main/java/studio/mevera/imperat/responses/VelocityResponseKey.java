package studio.mevera.imperat.responses;

/**
 * Response keys specific to the Velocity platform.
 * These keys are used to identify error messages and responses
 * for Velocity-specific exceptions.
 */
public interface VelocityResponseKey extends ResponseKey {

    // Source restrictions
    VelocityResponseKey ONLY_PLAYER = () -> "commands.conditions.only-player";
    VelocityResponseKey ONLY_CONSOLE = () -> "commands.conditions.only-console";

    // Entity/Player exceptions
    VelocityResponseKey UNKNOWN_PLAYER = () -> "args.parsing.unknown-player";

    // Server exceptions
    VelocityResponseKey UNKNOWN_SERVER = () -> "args.parsing.unknown-server";

}

