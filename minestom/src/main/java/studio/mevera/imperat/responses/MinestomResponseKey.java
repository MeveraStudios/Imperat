package studio.mevera.imperat.responses;

/**
 * Response keys specific to the Minestom platform.
 * These keys are used to identify error messages and responses
 * for Minestom-specific exceptions.
 */
public interface MinestomResponseKey extends ResponseKey {

    // Source restrictions
    MinestomResponseKey ONLY_PLAYER = () -> "commands.conditions.only-player";
    MinestomResponseKey ONLY_CONSOLE = () -> "commands.conditions.only-console";

    // Entity/Player exceptions
    MinestomResponseKey UNKNOWN_PLAYER = () -> "args.parsing.unknown-player";

    // Type parsing keys (one per registered Minestom argument type)
    MinestomResponseKey INVALID_STRING = () -> "args.parsing.invalid-string";
    MinestomResponseKey INVALID_INTEGER = () -> "args.parsing.invalid-integer";
    MinestomResponseKey INVALID_DOUBLE = () -> "args.parsing.invalid-double";
    MinestomResponseKey INVALID_FLOAT = () -> "args.parsing.invalid-float";
    MinestomResponseKey INVALID_LONG = () -> "args.parsing.invalid-long";

    // Minestom-specific type parsing keys
    MinestomResponseKey INVALID_COLOR = () -> "args.parsing.invalid-color";
    MinestomResponseKey INVALID_TIME = () -> "args.parsing.invalid-time";
    MinestomResponseKey INVALID_PARTICLE = () -> "args.parsing.invalid-particle";
    MinestomResponseKey INVALID_BLOCK_STATE = () -> "args.parsing.invalid-block-state";
    MinestomResponseKey INVALID_ITEM_STACK = () -> "args.parsing.invalid-item-stack";
    MinestomResponseKey INVALID_COMPONENT = () -> "args.parsing.invalid-component";
    MinestomResponseKey INVALID_ENTITY = () -> "args.parsing.invalid-entity";

    // Coordinate type parsing keys
    MinestomResponseKey INVALID_RELATIVE_VEC = () -> "args.parsing.invalid-relative-vec";
    MinestomResponseKey INVALID_BLOCK_POSITION = () -> "args.parsing.invalid-block-position";

    // NBT type parsing keys
    MinestomResponseKey INVALID_NBT = () -> "args.parsing.invalid-nbt";
    MinestomResponseKey INVALID_NBT_COMPOUND = () -> "args.parsing.invalid-nbt-compound";

    // Resource location parsing key
    MinestomResponseKey INVALID_RESOURCE_LOCATION = () -> "args.parsing.invalid-resource-location";

}

