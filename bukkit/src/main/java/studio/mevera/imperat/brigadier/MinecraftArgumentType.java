package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitUtil;
import studio.mevera.imperat.Version;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum MinecraftArgumentType {

    /**
     * A selector, player name, or UUID.
     */
    ENTITY_SELECTOR("ArgumentEntity", "commands.arguments.selector.EntitySelector", 1, boolean.class, boolean.class),

    /**
     * A player, online or not.
     */
    GAME_PROFILE("ArgumentProfile", "com.mojang.authlib.GameProfile", 1),

    /**
     * A chat color.
     */
    COLOR("ArgumentChatFormat", "ChatFormatting", 1),

    /**
     * A JSON Chat component.
     */
    COMPONENT("ArgumentChatComponent", "network.chat.Component", 1),

    /**
     * A regular message.
     */
    MESSAGE("ArgumentChat", "network.chat.Component", 1),

    /**
     * An NBT value.
     */
    NBT("ArgumentNBTTag", "nbt.CompoundTag", 1),

    /**
     * Represents a partial NBT tag.
     */
    NBT_TAG("ArgumentNBTBase", "nbt.Tag", 1),

    /**
     * A path within an NBT value.
     */
    NBT_PATH("ArgumentNBTKey", "commands.arguments.nbt.NbtPathArgument$NbtPath", 1),

    /**
     * A scoreboard objective.
     */
    SCOREBOARD_OBJECTIVE("ArgumentScoreboardObjective", "world.scores.Objective", 1),

    /**
     * A single score criterion.
     */
    OBJECTIVE_CRITERIA("ArgumentScoreboardCriteria", "world.scores.criteria.ObjectiveCriteria", 1),

    /**
     * A scoreboard slot.
     */
    SCOREBOARD_SLOT("ArgumentScoreboardSlot", "commands.arguments.ScoreboardSlotArgument$Result", 1),

    /**
     * Something that can join a team.
     */
    SCORE_HOLDER("ArgumentScoreholder", "java.lang.String", 1),

    /**
     * The name of a team.
     */
    TEAM("ArgumentScoreboardTeam", "java.lang.String", 1),

    /**
     * A scoreboard operation.
     */
    OPERATION("ArgumentMathOperation", "commands.arguments.OperationArgument$Operation", 1),

    /**
     * A particle effect.
     */
    PARTICLE("ArgumentParticle", "core.particles.ParticleOptions", 1),

    /**
     * Represents an angle.
     */
    ANGLE("ArgumentAngle", "java.lang.Float", 1),

    /**
     * A name for an inventory slot.
     */
    ITEM_SLOT("ArgumentInventorySlot", "java.lang.Integer", 1),

    /**
     * An Identifier.
     */
    RESOURCE_LOCATION("ArgumentMinecraftKeyRegistered", "resources.ResourceLocation", 1),

    /**
     * A potion effect.
     */
    POTION_EFFECT("ArgumentMobEffect", "world.effect.MobEffect", 1),

    /**
     * Represents an enchantment.
     */
    ENCHANTMENT("ArgumentEnchantment", "world.item.enchantment.Enchantment", 1),

    /**
     * Represents an entity summon.
     */
    ENTITY_SUMMON("ArgumentEntitySummon", "resources.ResourceLocation", 1),

    /**
     * Represents a dimension.
     */
    DIMENSION("ArgumentDimension", "resources.ResourceLocation", 1),

    /**
     * Represents a time duration.
     */
    TIME("ArgumentTime", "java.lang.Integer", 1),

    /**
     * Represents a UUID value.
     */
    UUID("ArgumentUUID", "java.util.UUID", 1),

    /**
     * A block position (x, y, z).
     */
    BLOCK_POS("coordinates.ArgumentPosition", "core.BlockPos", 3),

    /**
     * A column position (x, z).
     */
    COLUMN_POS("coordinates.ArgumentVec2I", "core.Vec2I", 2),

    /**
     * A 3D vector.
     */
    VECTOR_3("coordinates.ArgumentVec3", "world.phys.Vec3", 3),

    /**
     * A 2D vector.
     */
    VECTOR_2("coordinates.ArgumentVec2", "world.phys.Vec2", 2),

    /**
     * A rotation (yaw, pitch).
     */
    ROTATION("coordinates.ArgumentRotation", "commands.arguments.coordinates.RotationArgument$Rotation", 2),

    /**
     * A collection of up to 3 axes.
     */
    SWIZZLE("coordinates.ArgumentRotationAxis", "java.util.EnumSet", 1),

    /**
     * A block state.
     */
    BLOCK_STATE("blocks.ArgumentTile", "world.level.block.state.BlockState", 1),

    /**
     * A block predicate.
     */
    BLOCK_PREDICATE("blocks.ArgumentBlockPredicate", "commands.arguments.blocks.BlockPredicateArgument$Result", 1),

    /**
     * An item stack.
     */
    ITEM_STACK("item.ArgumentItemStack", "world.item.ItemStack", 1),

    /**
     * An item predicate.
     */
    ITEM_PREDICATE("item.ArgumentItemPredicate", "commands.arguments.item.ItemPredicateArgument$Result", 1),

    /**
     * A function.
     */
    FUNCTION("item.ArgumentTag", "resources.ResourceLocation", 1),

    /**
     * Entity anchor (feet/eyes).
     */
    ENTITY_ANCHOR("ArgumentAnchor", "commands.arguments.EntityAnchorArgument$Anchor", 1),

    /**
     * An integer range.
     */
    INT_RANGE("ArgumentCriterionValue$b", "commands.arguments.RangeArgument$IntRange", 1),

    /**
     * A floating-point range.
     */
    FLOAT_RANGE("ArgumentCriterionValue$a", "commands.arguments.RangeArgument$FloatRange", 1),

    /**
     * Template mirror.
     */
    TEMPLATE_MIRROR("TemplateMirrorArgument", "world.level.block.Mirror", 1),

    /**
     * Template rotation.
     */
    TEMPLATE_ROTATION("TemplateRotationArgument", "world.level.block.Rotation", 1);

    private final int consumedArgs;
    private final Class<?>[] parameters;
    private final @Nullable Class<?> parsedType;
    private @Nullable ArgumentType<?> argumentType;
    private @Nullable Constructor<? extends ArgumentType> argumentConstructor;

    MinecraftArgumentType(String name, String parsedTypePath, int consumedArgs, Class<?>... parameters) {
        this.consumedArgs = consumedArgs;
        this.parameters = parameters;

        // Resolve the argument parser class (existing logic)
        Class<?> argumentClass = resolveArgumentClass(name);
        if (argumentClass == null) {
            argumentType = null;
            argumentConstructor = null;
        } else {
            try {
                argumentConstructor = argumentClass.asSubclass(ArgumentType.class).getDeclaredConstructor(parameters);
                if (!argumentConstructor.isAccessible()) {
                    argumentConstructor.setAccessible(true);
                }
                if (parameters.length == 0) {
                    argumentType = argumentConstructor.newInstance();
                } else {
                    argumentType = null;
                }
            } catch (Throwable e) {
                argumentType = null;
                argumentConstructor = null;
            }
        }

        // Resolve the parsed type class using reflection
        this.parsedType = resolveParsedClass(parsedTypePath);
    }

    private static @Nullable Class<?> resolveArgumentClass(String name) {
        try {
            if (Version.isOver(1, 16, 5)) {
                return BukkitUtil.ClassesRefUtil.mcClass("commands.arguments." + name);
            } else {
                String stripped;
                if (name.lastIndexOf('.') != -1) {
                    stripped = name.substring(name.lastIndexOf('.') + 1);
                } else {
                    stripped = name;
                }
                return BukkitUtil.ClassesRefUtil.nmsClass(stripped);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Resolves the parsed type class from its Minecraft package path.
     *
     * @param path The class path (e.g., "core.BlockPos", "java.lang.String", or "commands.arguments.selector.EntitySelector")
     * @return The resolved class, or null if not found
     */
    private static @Nullable Class<?> resolveParsedClass(String path) {
        try {
            // Handle standard Java classes directly
            if (path.startsWith("java.")) {
                return Class.forName(path);
            }

            // Handle Minecraft classes using the existing util
            if (Version.isOver(1, 16, 5)) {
                return BukkitUtil.ClassesRefUtil.mcClass(path);
            } else {
                // For legacy versions, strip package and use nmsClass
                String stripped;
                if (path.lastIndexOf('.') != -1) {
                    stripped = path.substring(path.lastIndexOf('.') + 1);
                } else {
                    stripped = path;
                }
                return BukkitUtil.ClassesRefUtil.nmsClass(stripped);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static void ensureSetup() {
        // do nothing - this is only called to trigger the static initializer
    }

    /**
     * Returns the number of raw input arguments this argument type consumes.
     */
    public int getConsumedArgs() {
        return consumedArgs;
    }

    /**
     * Returns the Class representing the type that this argument parses into.
     * For example, BLOCK_POS returns BlockPos.class, ENTITY returns EntitySelector.class, etc.
     *
     * @param <T> The expected type
     * @return The Class object, or null if not resolvable in current version
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable Class<T> getParsedType() {
        return (Class<T>) parsedType;
    }

    /**
     * Checks if this argument valueType is supported in this Minecraft version
     */
    public boolean isSupported() {
        return argumentConstructor != null;
    }

    /**
     * Checks if this argument valueType requires parameters
     */
    public boolean requiresParameters() {
        return parameters.length != 0;
    }

    public @NotNull <T> ArgumentType<T> get() {
        if (argumentConstructor == null) {
            throw new IllegalArgumentException("Argument valueType '" + name().toLowerCase() + "' is not available on this version.");
        }
        if (argumentType != null) {
            return (ArgumentType<T>) argumentType;
        }
        throw new IllegalArgumentException("This argument valueType requires " + parameters.length + " parameter(s) of valueType(s) " +
                                                   Arrays.stream(parameters).map(Class::getName).collect(Collectors.joining(", "))
                                                   + ". Use #create() instead.");
    }

    public @NotNull <T> ArgumentType<T> create(Object... arguments) {
        if (argumentConstructor == null) {
            throw new IllegalArgumentException("Argument valueType '" + name().toLowerCase() + "' is not available on this version.");
        }

        if (argumentType != null && arguments.length == 0) {
            return (ArgumentType<T>) argumentType;
        }

        try {
            return (ArgumentType<T>) (argumentType = argumentConstructor.newInstance(arguments));
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull <T> Optional<ArgumentType<T>> getIfPresent() {
        if (argumentConstructor == null) {
            return Optional.empty();
        }
        if (argumentType != null) {
            return Optional.of((ArgumentType<T>) argumentType);
        }
        throw new IllegalArgumentException("This argument valueType requires " + parameters.length + " parameter(s) of valueType(s) " +
                                                   Arrays.stream(parameters).map(Class::getName).collect(Collectors.joining(", "))
                                                   + ". Use #create() instead.");
    }

    public @NotNull <T> Optional<ArgumentType<T>> createIfPresent(Object... arguments) {
        if (argumentConstructor == null) {
            return Optional.empty();
        }

        if (argumentType != null && arguments.length == 0) {
            return Optional.of((ArgumentType<T>) argumentType);
        }

        try {
            return Optional.of(argumentConstructor.newInstance(arguments));
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

}