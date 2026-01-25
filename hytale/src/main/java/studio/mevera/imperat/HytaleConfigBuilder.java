package studio.mevera.imperat;

import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
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
import studio.mevera.imperat.exception.InvalidIntegerOperator;
import studio.mevera.imperat.exception.InvalidLocationFormatException;
import studio.mevera.imperat.exception.OnlyConsoleAllowedException;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.type.HytaleParameterType;
import studio.mevera.imperat.type.ParameterLocation;
import studio.mevera.imperat.type.ParameterPlayer;
import studio.mevera.imperat.type.ParameterWorld;
import studio.mevera.imperat.util.TypeWrap;

public final class HytaleConfigBuilder extends ConfigBuilder<HytaleSource, HytaleImperat, HytaleConfigBuilder> {

    private static final HytaleParameterType.Data<?>[] HYTALE_ARGUMENT_TYPES = {
            //TODO we should add exceptions(and their providers) for each type of data eventually.

            /* new HytaleParameterType.Data<>(Boolean.class, ArgTypes.BOOLEAN, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(Integer.class, ArgTypes.INTEGER, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(String.class, ArgTypes.STRING, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(Float.class, ArgTypes.FLOAT, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(Double.class, ArgTypes.DOUBLE, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(UUID.class, ArgTypes.UUID, HytaleParameterType.ExceptionProvider.DEFAULT), */

            // new HytaleParameterType.Data<>(PlayerRef.class, ArgTypes.PLAYER_REF, HytaleParameterType.ExceptionProvider.DEFAULT),
            // new HytaleParameterType.Data<>(World.class, ArgTypes.WORLD, HytaleParameterType.ExceptionProvider.DEFAULT),

            new HytaleParameterType.Data<>(Coord.class, ArgTypes.RELATIVE_DOUBLE_COORD, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(IntCoord.class, ArgTypes.RELATIVE_INT_COORD, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(RelativeInteger.class, ArgTypes.RELATIVE_INTEGER, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(RelativeFloat.class, ArgTypes.RELATIVE_FLOAT, HytaleParameterType.ExceptionProvider.DEFAULT),

            new HytaleParameterType.Data<>(Vector2i.class, ArgTypes.VECTOR2I, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(Vector3i.class, ArgTypes.VECTOR3I, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(RelativeVector3i.class, ArgTypes.RELATIVE_VECTOR3I, HytaleParameterType.ExceptionProvider.DEFAULT),

            new HytaleParameterType.Data<>(RelativeIntPosition.class, ArgTypes.RELATIVE_BLOCK_POSITION,
                    HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(RelativeDoublePosition.class, ArgTypes.RELATIVE_POSITION, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(RelativeChunkPosition.class, ArgTypes.RELATIVE_CHUNK_POSITION,
                    HytaleParameterType.ExceptionProvider.DEFAULT),

            new HytaleParameterType.Data<>(Vector3f.class, ArgTypes.ROTATION, HytaleParameterType.ExceptionProvider.DEFAULT),

            new HytaleParameterType.Data<>(ModelAsset.class, ArgTypes.MODEL_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(Weather.class, ArgTypes.WEATHER_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(Interaction.class, ArgTypes.INTERACTION_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(RootInteraction.class, ArgTypes.ROOT_INTERACTION_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(EntityEffect.class, ArgTypes.EFFECT_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(Environment.class, ArgTypes.ENVIRONMENT_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(Item.class, ArgTypes.ITEM_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(BlockType.class, ArgTypes.BLOCK_TYPE_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(ParticleSystem.class, ArgTypes.PARTICLE_SYSTEM, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(HitboxCollisionConfig.class, ArgTypes.HITBOX_COLLISION_CONFIG, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(RepulsionConfig.class, ArgTypes.REPULSION_CONFIG, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(SoundEvent.class, ArgTypes.SOUND_EVENT_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(AmbienceFX.class, ArgTypes.AMBIENCE_FX_ASSET, HytaleParameterType.ExceptionProvider.DEFAULT),

            new HytaleParameterType.Data<>(SoundCategory.class, ArgTypes.SOUND_CATEGORY, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(GameMode.class, ArgTypes.GAME_MODE, HytaleParameterType.ExceptionProvider.DEFAULT),

            new HytaleParameterType.Data<>(BlockMask.class, ArgTypes.BLOCK_MASK, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(BlockPattern.class, ArgTypes.BLOCK_PATTERN, HytaleParameterType.ExceptionProvider.DEFAULT),

            new HytaleParameterType.Data<>(ArgTypes.IntegerComparisonOperator.class, ArgTypes.INTEGER_COMPARISON_OPERATOR, HytaleParameterType.ExceptionProvider.DEFAULT),
            new HytaleParameterType.Data<>(ArgTypes.IntegerOperation.class, ArgTypes.INTEGER_OPERATION, InvalidIntegerOperator::new)

            // new HytaleParameterType.Data<>(Pair.class, ArgTypes.INT_RANGE, HytaleParameterType.ExceptionProvider.DEFAULT) // shared Pair<Integer,
            // Integer>
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
        this.addThrowableHandlers();
    }

    /**
     * Registers context resolvers for automatic dependency injection in commands.
     * This allows command methods to receive Minestom-specific objects as parameters.
     */
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<HytaleSource>>() {}.getType(),
                (ctx, paramElement) -> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<HytaleSource>>() {}.getType(),
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
                throw new OnlyConsoleAllowedException();
            }
            return (ConsoleSender) minestomSource.origin();
        });

        config.registerSourceResolver(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.as(Player.class);
        });

        config.registerSourceResolver(PlayerRef.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.asPlayerRef();
        });
    }

    private void registerDefaultParamTypes() {
        config.registerParamType(Location.class, new ParameterLocation());
        config.registerParamType(PlayerRef.class, new ParameterPlayer());
        config.registerParamType(World.class, new ParameterWorld());

        // Registerall other types
        for (HytaleParameterType.Data<?> data : HYTALE_ARGUMENT_TYPES) {
            config.registerParamType(data.type(), new HytaleParameterType<>(data));
        }

    }

    /**
     * Registers exception handlers for common Hytale command scenarios.
     * This provides user-friendly error messages for various error conditions.
     */
    private void addThrowableHandlers() {
        config.setThrowableResolver(
                OnlyPlayerAllowedException.class,
                (ex, context) -> context.source().error("Only players can do this!")
        );

        config.setThrowableResolver(
                OnlyConsoleAllowedException.class,
                (ex, context) -> context.source().error("Only console can do this!")
        );

        config.setThrowableResolver(
                UnknownPlayerException.class,
                (exception, context) ->
                        context.source().origin().sendMessage(
                                Message.translation("server.commands.errors.noSuchPlayer").param("username", exception.getName())
                        )
        );

        config.setThrowableResolver(InvalidLocationFormatException.class, (exception, context) -> {
            InvalidLocationFormatException.Reason reason = exception.getReason();
            String msg = switch (reason) {
                case INVALID_X_COORDINATE -> "Invalid X coordinate '" + exception.getInputX() + "'";
                case INVALID_Y_COORDINATE -> "Invalid Y coordinate '" + exception.getInputY() + "'";
                case INVALID_Z_COORDINATE -> "Invalid Z coordinate '" + exception.getInputZ() + "'";
                case INVALID_YAW_COORDINATE -> "Invalid Yaw coordinate '" + exception.getInputYaw() + "'";
                case INVALID_PITCH_COORDINATE -> "Invalid Pitch coordinate '" + exception.getInputPitch() + "'";
                case NO_WORLDS_AVAILABLE -> "Failed to fetch the world of the given location";
                case WRONG_FORMAT -> "Wrong location format!";
                case SELF_LOCATION_NOT_AVAILABLE -> null;
            };

            context.source().reply("&4Failed to parse location '" + exception.getInput() + "' due to: &c" + msg);
        });
        config.setThrowableResolver(InvalidIntegerOperator.class,
                (ex, ctx) ->
                        ctx.source().origin().sendMessage(
                                Message.raw("Could not find an integer operator for value: '" + ex.getInput() + "'.")
                        )
        );
    }

    @Override
    public @NotNull HytaleImperat build() {
        return new HytaleImperat(plugin, config);
    }
}
