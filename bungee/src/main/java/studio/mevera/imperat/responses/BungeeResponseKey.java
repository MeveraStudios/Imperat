package studio.mevera.imperat.responses;

/**
 * Response keys specific to the Bungee platform.
 * These keys are used to identify error messages and responses
 * for Bungee-specific exceptions.
 */
public interface BungeeResponseKey extends ResponseKey {

    // Source restrictions
    BungeeResponseKey ONLY_PLAYER = () -> "commands.conditions.only-player";
    BungeeResponseKey ONLY_CONSOLE = () -> "commands.conditions.only-console";

    // Entity/Player exceptions
    BungeeResponseKey UNKNOWN_PLAYER = () -> "args.parsing.unknown-player";

    // Server exceptions
    BungeeResponseKey UNKNOWN_SERVER = () -> "args.parsing.unknown-server";

}

