package studio.mevera.imperat.responses;

/**
 * Response keys specific to the Bukkit platform.
 * These keys are used to identify error messages and responses
 * for Bukkit-specific exceptions.
 */
public interface BukkitResponseKey extends ResponseKey {

    // Source restrictions
    BukkitResponseKey ONLY_PLAYER = () -> "only-player";
    BukkitResponseKey ONLY_CONSOLE = () -> "only-console";

    // Entity/Player exceptions
    BukkitResponseKey UNKNOWN_PLAYER = () -> "unknown-player";
    BukkitResponseKey UNKNOWN_OFFLINE_PLAYER = () -> "unknown-offline-player";

    // World exceptions
    BukkitResponseKey UNKNOWN_WORLD = () -> "unknown-world";

    // Location exceptions
    BukkitResponseKey INVALID_LOCATION = () -> "invalid-location";

    // Selector exceptions
    BukkitResponseKey INVALID_SELECTOR_FIELD = () -> "invalid-selector-field";
    BukkitResponseKey UNKNOWN_SELECTOR_FIELD = () -> "unknown-selector-field";
    BukkitResponseKey UNKNOWN_SELECTION_TYPE = () -> "unknown-selection-type";

}

