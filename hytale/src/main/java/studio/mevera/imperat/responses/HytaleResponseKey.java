package studio.mevera.imperat.responses;

/**
 * Response keys specific to the Hytale platform.
 * These keys are used to identify error messages and responses
 * for Hytale-specific exceptions.
 */
public interface HytaleResponseKey extends ResponseKey {

    // Source restrictions
    HytaleResponseKey ONLY_PLAYER = () -> "commands.conditions.only-player";
    HytaleResponseKey ONLY_CONSOLE = () -> "commands.conditions.only-console";

    // Entity/Player exceptions
    HytaleResponseKey UNKNOWN_PLAYER = () -> "args.parsing.unknown-player";

    // World exceptions
    HytaleResponseKey UNKNOWN_WORLD = () -> "args.parsing.unknown-world";

    // Location exceptions
    HytaleResponseKey INVALID_LOCATION = () -> "args.parsing.invalid-location";

    // Coordinate argument type parsing errors
    HytaleResponseKey INVALID_RELATIVE_DOUBLE_COORD = () -> "args.parsing.invalid-relative-double-coord";
    HytaleResponseKey INVALID_RELATIVE_INT_COORD = () -> "args.parsing.invalid-relative-int-coord";
    HytaleResponseKey INVALID_RELATIVE_INTEGER = () -> "args.parsing.invalid-relative-integer";
    HytaleResponseKey INVALID_RELATIVE_FLOAT = () -> "args.parsing.invalid-relative-float";

    // Vector argument type parsing errors
    HytaleResponseKey INVALID_VECTOR2I = () -> "args.parsing.invalid-vector2i";
    HytaleResponseKey INVALID_VECTOR3I = () -> "args.parsing.invalid-vector3i";
    HytaleResponseKey INVALID_RELATIVE_VECTOR3I = () -> "args.parsing.invalid-relative-vector3i";

    // Position argument type parsing errors
    HytaleResponseKey INVALID_RELATIVE_BLOCK_POSITION = () -> "args.parsing.invalid-relative-block-position";
    HytaleResponseKey INVALID_RELATIVE_POSITION = () -> "args.parsing.invalid-relative-position";
    HytaleResponseKey INVALID_RELATIVE_CHUNK_POSITION = () -> "args.parsing.invalid-relative-chunk-position";

    // Rotation argument type parsing errors
    HytaleResponseKey INVALID_ROTATION = () -> "args.parsing.invalid-rotation";

    // Asset argument type parsing errors
    HytaleResponseKey INVALID_MODEL_ASSET = () -> "args.parsing.invalid-model-asset";
    HytaleResponseKey INVALID_WEATHER_ASSET = () -> "args.parsing.invalid-weather-asset";
    HytaleResponseKey INVALID_INTERACTION_ASSET = () -> "args.parsing.invalid-interaction-asset";
    HytaleResponseKey INVALID_ROOT_INTERACTION_ASSET = () -> "args.parsing.invalid-root-interaction-asset";
    HytaleResponseKey INVALID_EFFECT_ASSET = () -> "args.parsing.invalid-effect-asset";
    HytaleResponseKey INVALID_ENVIRONMENT_ASSET = () -> "args.parsing.invalid-environment-asset";
    HytaleResponseKey INVALID_ITEM_ASSET = () -> "args.parsing.invalid-item-asset";
    HytaleResponseKey INVALID_BLOCK_TYPE_ASSET = () -> "args.parsing.invalid-block-type-asset";
    HytaleResponseKey INVALID_PARTICLE_SYSTEM = () -> "args.parsing.invalid-particle-system";
    HytaleResponseKey INVALID_HITBOX_COLLISION_CONFIG = () -> "args.parsing.invalid-hitbox-collision-config";
    HytaleResponseKey INVALID_REPULSION_CONFIG = () -> "args.parsing.invalid-repulsion-config";
    HytaleResponseKey INVALID_SOUND_EVENT_ASSET = () -> "args.parsing.invalid-sound-event-asset";
    HytaleResponseKey INVALID_AMBIENCE_FX_ASSET = () -> "args.parsing.invalid-ambience-fx-asset";

    // Enum argument type parsing errors
    HytaleResponseKey INVALID_SOUND_CATEGORY = () -> "args.parsing.invalid-sound-category";
    HytaleResponseKey INVALID_GAME_MODE = () -> "args.parsing.invalid-game-mode";

    // Selection argument type parsing errors
    HytaleResponseKey INVALID_BLOCK_MASK = () -> "args.parsing.invalid-block-mask";
    HytaleResponseKey INVALID_BLOCK_PATTERN = () -> "args.parsing.invalid-block-pattern";

    // Operator argument type parsing errors
    HytaleResponseKey INVALID_INTEGER_COMPARISON_OPERATOR = () -> "args.parsing.invalid-integer-comparison-operator";
    HytaleResponseKey INVALID_INTEGER_OPERATION = () -> "args.parsing.invalid-integer-operation";

}

