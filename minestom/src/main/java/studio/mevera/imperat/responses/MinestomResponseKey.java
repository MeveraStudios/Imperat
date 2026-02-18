package studio.mevera.imperat.responses;

/**
 * Response keys specific to the Minestom platform.
 * These keys are used to identify error messages and responses
 * for Minestom-specific exceptions.
 */
public interface MinestomResponseKey {

    // Source restrictions
    ResponseKey ONLY_PLAYER = () -> "only-player";
    ResponseKey ONLY_CONSOLE = () -> "only-console";

    // Entity/Player exceptions
    ResponseKey UNKNOWN_PLAYER = () -> "unknown-player";

}

