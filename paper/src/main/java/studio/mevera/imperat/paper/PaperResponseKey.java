package studio.mevera.imperat.paper;

import studio.mevera.imperat.responses.ResponseKey;

/**
 * Response keys for Paper-specific failure modes (player offline, unknown
 * world, invalid location, etc.). Mirrors the legacy
 * {@code BukkitResponseKey} but lives in the paper module so the two
 * paths stay independent.
 */
public interface PaperResponseKey extends ResponseKey {

    ResponseKey ONLY_PLAYER = () -> "paper.error.only-player";
    ResponseKey ONLY_CONSOLE = () -> "paper.error.only-console";
    ResponseKey UNKNOWN_PLAYER = () -> "paper.error.unknown-player";
    ResponseKey UNKNOWN_OFFLINE_PLAYER = () -> "paper.error.unknown-offline-player";
    ResponseKey UNKNOWN_WORLD = () -> "paper.error.unknown-world";
    ResponseKey INVALID_LOCATION = () -> "paper.error.invalid-location";
}
