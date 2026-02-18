package studio.mevera.imperat.responses;

/**
 * Response keys specific to the Bungee platform.
 * These keys are used to identify error messages and responses
 * for Bungee-specific exceptions.
 */
public interface BungeeResponseKey {

    // Source restrictions
    ResponseKey ONLY_PLAYER = () -> "only-player";
    ResponseKey ONLY_CONSOLE = () -> "only-console";

    // Entity/Player exceptions
    ResponseKey UNKNOWN_PLAYER = () -> "unknown-player";

    // Server exceptions
    ResponseKey UNKNOWN_SERVER = () -> "unknown-server";

}

