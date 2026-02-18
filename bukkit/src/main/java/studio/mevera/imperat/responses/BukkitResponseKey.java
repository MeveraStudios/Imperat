package studio.mevera.imperat.responses;

import studio.mevera.imperat.util.Keyed;

/**
 * Response keys specific to the Bukkit platform.
 * These keys are used to identify error messages and responses
 * for Bukkit-specific exceptions.
 */
public interface BukkitResponseKey extends Keyed<String> {

    // Source restrictions
    ResponseKey ONLY_PLAYER = () -> "only-player";
    ResponseKey ONLY_CONSOLE = () -> "only-console";

    // Entity/Player exceptions
    ResponseKey UNKNOWN_PLAYER = () -> "unknown-player";
    ResponseKey UNKNOWN_OFFLINE_PLAYER = () -> "unknown-offline-player";

    // World exceptions
    ResponseKey UNKNOWN_WORLD = () -> "unknown-world";

    // Location exceptions
    ResponseKey INVALID_LOCATION = () -> "invalid-location";

    // Selector exceptions
    ResponseKey INVALID_SELECTOR_FIELD = () -> "invalid-selector-field";
    ResponseKey UNKNOWN_SELECTOR_FIELD = () -> "unknown-selector-field";
    ResponseKey UNKNOWN_SELECTION_TYPE = () -> "unknown-selection-type";

}

