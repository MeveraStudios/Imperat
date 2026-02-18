package studio.mevera.imperat;

import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.ambiencefx.config.AmbienceFX;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.Coord;
import com.hypixel.hytale.server.core.command.system.arguments.types.IntCoord;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeChunkPosition;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeDoublePosition;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeFloat;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeIntPosition;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeInteger;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeVector3i;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig;
import com.hypixel.hytale.server.core.modules.entity.repulsion.RepulsionConfig;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.HytaleResponseKey;
import studio.mevera.imperat.type.HytaleArgumentType;
import studio.mevera.imperat.type.LocationArgument;
import studio.mevera.imperat.type.PlayerArgument;
import studio.mevera.imperat.type.WorldArgument;
import studio.mevera.imperat.util.TypeWrap;

public final class HytaleConfigBuilder extends ConfigBuilder<HytaleSource, HytaleImperat, HytaleConfigBuilder> {

    private static final HytaleArgumentType.Data<?>[] HYTALE_ARGUMENT_TYPES = {
            //TODO we should add exceptions(and their providers) for each type of data eventually.

            /* new HytaleArgumentType.Data<>(Boolean.class, ArgTypes.BOOLEAN, new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey
            .INVALID_BOOLEAN)),
            new HytaleArgumentType.Data<>(Integer.class, ArgTypes.INTEGER, new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey
            .INVALID_INTEGER)),
            new HytaleArgumentType.Data<>(String.class, ArgTypes.STRING, new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey
            .INVALID_STRING)),
            new HytaleArgumentType.Data<>(Float.class, ArgTypes.FLOAT, new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey
            .INVALID_FLOAT)),
            new HytaleArgumentType.Data<>(Double.class, ArgTypes.DOUBLE, new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey
            .INVALID_DOUBLE)),
            new HytaleArgumentType.Data<>(UUID.class, ArgTypes.UUID, new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey
            .INVALID_UUID)), */

            // new HytaleArgumentType.Data<>(PlayerRef.class, ArgTypes.PLAYER_REF, new HytaleArgumentType.ResponseKeyExceptionProvider
            // (HytaleResponseKey.UNKNOWN_PLAYER)),
            // new HytaleArgumentType.Data<>(World.class, ArgTypes.WORLD, new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey
            // .UNKNOWN_WORLD)),

            new HytaleArgumentType.Data<>(Coord.class, ArgTypes.RELATIVE_DOUBLE_COORD,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_RELATIVE_DOUBLE_COORD)),
            new HytaleArgumentType.Data<>(IntCoord.class, ArgTypes.RELATIVE_INT_COORD,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_RELATIVE_INT_COORD)),
            new HytaleArgumentType.Data<>(RelativeInteger.class, ArgTypes.RELATIVE_INTEGER,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_RELATIVE_INTEGER)),
            new HytaleArgumentType.Data<>(RelativeFloat.class, ArgTypes.RELATIVE_FLOAT,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_RELATIVE_FLOAT)),

            new HytaleArgumentType.Data<>(Vector2i.class, ArgTypes.VECTOR2I,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_VECTOR2I)),
            new HytaleArgumentType.Data<>(Vector3i.class, ArgTypes.VECTOR3I,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_VECTOR3I)),
            new HytaleArgumentType.Data<>(RelativeVector3i.class, ArgTypes.RELATIVE_VECTOR3I,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_RELATIVE_VECTOR3I)),

            new HytaleArgumentType.Data<>(RelativeIntPosition.class, ArgTypes.RELATIVE_BLOCK_POSITION,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_RELATIVE_BLOCK_POSITION)),
            new HytaleArgumentType.Data<>(RelativeDoublePosition.class, ArgTypes.RELATIVE_POSITION,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_RELATIVE_POSITION)),
            new HytaleArgumentType.Data<>(RelativeChunkPosition.class, ArgTypes.RELATIVE_CHUNK_POSITION,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_RELATIVE_CHUNK_POSITION)),

            new HytaleArgumentType.Data<>(Vector3f.class, ArgTypes.ROTATION,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_ROTATION)),

            new HytaleArgumentType.Data<>(ModelAsset.class, ArgTypes.MODEL_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_MODEL_ASSET)),
            new HytaleArgumentType.Data<>(Weather.class, ArgTypes.WEATHER_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_WEATHER_ASSET)),
            new HytaleArgumentType.Data<>(Interaction.class, ArgTypes.INTERACTION_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_INTERACTION_ASSET)),
            new HytaleArgumentType.Data<>(RootInteraction.class, ArgTypes.ROOT_INTERACTION_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_ROOT_INTERACTION_ASSET)),
            new HytaleArgumentType.Data<>(EntityEffect.class, ArgTypes.EFFECT_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_EFFECT_ASSET)),
            new HytaleArgumentType.Data<>(Environment.class, ArgTypes.ENVIRONMENT_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_ENVIRONMENT_ASSET)),
            new HytaleArgumentType.Data<>(Item.class, ArgTypes.ITEM_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_ITEM_ASSET)),
            new HytaleArgumentType.Data<>(BlockType.class, ArgTypes.BLOCK_TYPE_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_BLOCK_TYPE_ASSET)),
            new HytaleArgumentType.Data<>(ParticleSystem.class, ArgTypes.PARTICLE_SYSTEM,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_PARTICLE_SYSTEM)),
            new HytaleArgumentType.Data<>(HitboxCollisionConfig.class, ArgTypes.HITBOX_COLLISION_CONFIG,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_HITBOX_COLLISION_CONFIG)),
            new HytaleArgumentType.Data<>(RepulsionConfig.class, ArgTypes.REPULSION_CONFIG,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_REPULSION_CONFIG)),
            new HytaleArgumentType.Data<>(SoundEvent.class, ArgTypes.SOUND_EVENT_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_SOUND_EVENT_ASSET)),
            new HytaleArgumentType.Data<>(AmbienceFX.class, ArgTypes.AMBIENCE_FX_ASSET,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_AMBIENCE_FX_ASSET)),

            new HytaleArgumentType.Data<>(SoundCategory.class, ArgTypes.SOUND_CATEGORY,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_SOUND_CATEGORY)),
            new HytaleArgumentType.Data<>(GameMode.class, ArgTypes.GAME_MODE,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_GAME_MODE)),

            new HytaleArgumentType.Data<>(BlockMask.class, ArgTypes.BLOCK_MASK,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_BLOCK_MASK)),
            new HytaleArgumentType.Data<>(BlockPattern.class, ArgTypes.BLOCK_PATTERN,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_BLOCK_PATTERN)),

            new HytaleArgumentType.Data<>(ArgTypes.IntegerComparisonOperator.class, ArgTypes.INTEGER_COMPARISON_OPERATOR,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_INTEGER_COMPARISON_OPERATOR)),
            new HytaleArgumentType.Data<>(ArgTypes.IntegerOperation.class, ArgTypes.INTEGER_OPERATION,
                    new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey.INVALID_INTEGER_OPERATION))

            // new HytaleArgumentType.Data<>(Pair.class, ArgTypes.INT_RANGE, new HytaleArgumentType.ResponseKeyExceptionProvider(HytaleResponseKey
            // .INVALID_INT_RANGE)) // shared Pair<Integer, Integer>
    };

    private final JavaPlugin plugin;

    HytaleConfigBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
        this.permissionChecker((src, perm) -> {
            if (perm == null || src.isConsole()) {
                return true;
            }
            return src.asPlayer().hasPermission(perm);
        });
        this.registerContextResolvers();
        this.registerDefaultSourceResolvers();
        this.registerDefaultParamTypes();
        this.registerHytaleResponses();
    }

    /**
     * Registers context resolvers for automatic dependency injection in commands.
     * This allows command methods to receive Minestom-specific objects as parameters.
     */
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<HytaleSource>>() {
                }.getType(),
                (ctx, paramElement) -> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<HytaleSource>>() {
                }.getType(),
                (ctx, paramElement) -> CommandHelp.create(ctx)
        );

        config.registerContextResolver(JavaPlugin.class, (ctx, paramElement) -> plugin);
    }

    /**
     * Registers source resolvers for type-safe command source handling.
     * This enables automatic casting and validation of command sources.
     */
    private void registerDefaultSourceResolvers() {
        config.registerSourceResolver(CommandSender.class, (minestomSource, ctx) -> minestomSource.origin());

        config.registerSourceResolver(ConsoleSender.class, (minestomSource, ctx) -> {
            if (!minestomSource.isConsole()) {
                throw new CommandException(HytaleResponseKey.ONLY_CONSOLE);
            }
            return (ConsoleSender) minestomSource.origin();
        });

        config.registerSourceResolver(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new CommandException(HytaleResponseKey.ONLY_PLAYER);
            }
            return source.as(Player.class);
        });

        config.registerSourceResolver(PlayerRef.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new CommandException(HytaleResponseKey.ONLY_PLAYER);
            }
            return source.asPlayerRef();
        });
    }

    private void registerDefaultParamTypes() {
        config.registerArgType(Location.class, new LocationArgument());
        config.registerArgType(PlayerRef.class, new PlayerArgument());
        config.registerArgType(World.class, new WorldArgument());

        // Registerall other types
        for (HytaleArgumentType.Data<?> data : HYTALE_ARGUMENT_TYPES) {
            config.registerArgType(data.type(), new HytaleArgumentType<>(data));
        }

    }

    /**
     * Registers responses for common Hytale command scenarios.
     * This provides user-friendly error messages for various error conditions.
     */
    private void registerHytaleResponses() {
        var responseRegistry = config.getResponseRegistry();

        // Register responses for Hytale-specific exceptions
        responseRegistry.registerResponse(
                HytaleResponseKey.ONLY_PLAYER,
                () -> "Only players can do this!"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.ONLY_CONSOLE,
                () -> "Only console can do this!"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.UNKNOWN_PLAYER,
                () -> "Player '%username%' not found"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.UNKNOWN_WORLD,
                () -> "World '%name%' not found"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_LOCATION,
                () -> "&4Failed to parse location '%input%' due to: &c%message%"
        );

        // Coordinate parsing errors
        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_RELATIVE_DOUBLE_COORD,
                () -> "Invalid relative double coordinate: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_RELATIVE_INT_COORD,
                () -> "Invalid relative integer coordinate: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_RELATIVE_INTEGER,
                () -> "Invalid relative integer: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_RELATIVE_FLOAT,
                () -> "Invalid relative float: '%input%'"
        );

        // Vector parsing errors
        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_VECTOR2I,
                () -> "Invalid 2D vector: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_VECTOR3I,
                () -> "Invalid 3D vector: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_RELATIVE_VECTOR3I,
                () -> "Invalid relative 3D vector: '%input%'"
        );

        // Position parsing errors
        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_RELATIVE_BLOCK_POSITION,
                () -> "Invalid relative block position: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_RELATIVE_POSITION,
                () -> "Invalid relative position: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_RELATIVE_CHUNK_POSITION,
                () -> "Invalid relative chunk position: '%input%'"
        );

        // Rotation parsing errors
        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_ROTATION,
                () -> "Invalid rotation: '%input%'"
        );

        // Asset parsing errors
        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_MODEL_ASSET,
                () -> "Invalid model asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_WEATHER_ASSET,
                () -> "Invalid weather asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_INTERACTION_ASSET,
                () -> "Invalid interaction asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_ROOT_INTERACTION_ASSET,
                () -> "Invalid root interaction asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_EFFECT_ASSET,
                () -> "Invalid effect asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_ENVIRONMENT_ASSET,
                () -> "Invalid environment asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_ITEM_ASSET,
                () -> "Invalid item asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_BLOCK_TYPE_ASSET,
                () -> "Invalid block type asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_PARTICLE_SYSTEM,
                () -> "Invalid particle system: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_HITBOX_COLLISION_CONFIG,
                () -> "Invalid hitbox collision config: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_REPULSION_CONFIG,
                () -> "Invalid repulsion config: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_SOUND_EVENT_ASSET,
                () -> "Invalid sound event asset: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_AMBIENCE_FX_ASSET,
                () -> "Invalid ambience FX asset: '%input%'"
        );

        // Enum parsing errors
        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_SOUND_CATEGORY,
                () -> "Invalid sound category: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_GAME_MODE,
                () -> "Invalid game mode: '%input%'"
        );

        // Selection parsing errors
        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_BLOCK_MASK,
                () -> "Invalid block mask: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_BLOCK_PATTERN,
                () -> "Invalid block pattern: '%input%'"
        );

        // Operator parsing errors
        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_INTEGER_COMPARISON_OPERATOR,
                () -> "Invalid integer comparison operator: '%input%'"
        );

        responseRegistry.registerResponse(
                HytaleResponseKey.INVALID_INTEGER_OPERATION,
                () -> "Invalid integer operation: '%input%'"
        );
    }

    @Override
    public @NotNull HytaleImperat build() {
        return new HytaleImperat(plugin, config);
    }
}
