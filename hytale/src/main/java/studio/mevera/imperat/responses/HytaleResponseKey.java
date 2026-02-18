package studio.mevera.imperat.responses;

/**
 * Response keys specific to the Hytale platform.
 * These keys are used to identify error messages and responses
 * for Hytale-specific exceptions.
 */
public interface HytaleResponseKey {

    // Source restrictions
    ResponseKey ONLY_PLAYER = () -> "only-player";
    ResponseKey ONLY_CONSOLE = () -> "only-console";

    // Entity/Player exceptions
    ResponseKey UNKNOWN_PLAYER = () -> "unknown-player";

    // World exceptions
    ResponseKey UNKNOWN_WORLD = () -> "unknown-world";

    // Location exceptions
    ResponseKey INVALID_LOCATION = () -> "invalid-location";

    // Coordinate argument type parsing errors
    ResponseKey INVALID_RELATIVE_DOUBLE_COORD = () -> "args.parsing.invalid-relative-double-coord";
    ResponseKey INVALID_RELATIVE_INT_COORD = () -> "args.parsing.invalid-relative-int-coord";
    ResponseKey INVALID_RELATIVE_INTEGER = () -> "args.parsing.invalid-relative-integer";
    ResponseKey INVALID_RELATIVE_FLOAT = () -> "args.parsing.invalid-relative-float";

    // Vector argument type parsing errors
    ResponseKey INVALID_VECTOR2I = () -> "args.parsing.invalid-vector2i";
    ResponseKey INVALID_VECTOR3I = () -> "args.parsing.invalid-vector3i";
    ResponseKey INVALID_RELATIVE_VECTOR3I = () -> "args.parsing.invalid-relative-vector3i";

    // Position argument type parsing errors
    ResponseKey INVALID_RELATIVE_BLOCK_POSITION = () -> "args.parsing.invalid-relative-block-position";
    ResponseKey INVALID_RELATIVE_POSITION = () -> "args.parsing.invalid-relative-position";
    ResponseKey INVALID_RELATIVE_CHUNK_POSITION = () -> "args.parsing.invalid-relative-chunk-position";

    // Rotation argument type parsing errors
    ResponseKey INVALID_ROTATION = () -> "args.parsing.invalid-rotation";

    // Asset argument type parsing errors
    ResponseKey INVALID_MODEL_ASSET = () -> "args.parsing.invalid-model-asset";
    ResponseKey INVALID_WEATHER_ASSET = () -> "args.parsing.invalid-weather-asset";
    ResponseKey INVALID_INTERACTION_ASSET = () -> "args.parsing.invalid-interaction-asset";
    ResponseKey INVALID_ROOT_INTERACTION_ASSET = () -> "args.parsing.invalid-root-interaction-asset";
    ResponseKey INVALID_EFFECT_ASSET = () -> "args.parsing.invalid-effect-asset";
    ResponseKey INVALID_ENVIRONMENT_ASSET = () -> "args.parsing.invalid-environment-asset";
    ResponseKey INVALID_ITEM_ASSET = () -> "args.parsing.invalid-item-asset";
    ResponseKey INVALID_BLOCK_TYPE_ASSET = () -> "args.parsing.invalid-block-type-asset";
    ResponseKey INVALID_PARTICLE_SYSTEM = () -> "args.parsing.invalid-particle-system";
    ResponseKey INVALID_HITBOX_COLLISION_CONFIG = () -> "args.parsing.invalid-hitbox-collision-config";
    ResponseKey INVALID_REPULSION_CONFIG = () -> "args.parsing.invalid-repulsion-config";
    ResponseKey INVALID_SOUND_EVENT_ASSET = () -> "args.parsing.invalid-sound-event-asset";
    ResponseKey INVALID_AMBIENCE_FX_ASSET = () -> "args.parsing.invalid-ambience-fx-asset";

    // Enum argument type parsing errors
    ResponseKey INVALID_SOUND_CATEGORY = () -> "args.parsing.invalid-sound-category";
    ResponseKey INVALID_GAME_MODE = () -> "args.parsing.invalid-game-mode";

    // Selection argument type parsing errors
    ResponseKey INVALID_BLOCK_MASK = () -> "args.parsing.invalid-block-mask";
    ResponseKey INVALID_BLOCK_PATTERN = () -> "args.parsing.invalid-block-pattern";

    // Operator argument type parsing errors
    ResponseKey INVALID_INTEGER_COMPARISON_OPERATOR = () -> "args.parsing.invalid-integer-comparison-operator";
    ResponseKey INVALID_INTEGER_OPERATION = () -> "args.parsing.invalid-integer-operation";

}

