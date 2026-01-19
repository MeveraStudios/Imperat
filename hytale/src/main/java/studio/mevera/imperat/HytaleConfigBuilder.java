package studio.mevera.imperat;

import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.exception.InvalidLocationFormatException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.OnlyConsoleAllowedException;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.type.ParameterLocation;
import studio.mevera.imperat.type.ParameterPlayer;
import studio.mevera.imperat.type.ParameterWorld;
import studio.mevera.imperat.util.TypeWrap;

public final class HytaleConfigBuilder extends ConfigBuilder<HytaleSource, HytaleImperat, HytaleConfigBuilder> {

    private final JavaPlugin plugin;

    HytaleConfigBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
        this.permissionChecker((src, perm)-> {
            if(perm == null || src.isConsole()) {
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
                (ctx, paramElement)-> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<HytaleSource>>() {}.getType(),
                (ctx, paramElement)-> CommandHelp.create(ctx)
        );

        config.registerContextResolver(JavaPlugin.class, (ctx, paramElement) -> plugin);
    }

    /**
     * Registers source resolvers for type-safe command source handling.
     * This enables automatic casting and validation of command sources.
     */
    private void registerDefaultSourceResolvers() {
        config.registerSourceResolver(CommandSender.class, (minestomSource, ctx) -> minestomSource.origin());

        // Enhanced source resolver for console similar to Velocity
        config.registerSourceResolver(ConsoleSender.class, (minestomSource, ctx) -> {
            if (!minestomSource.isConsole()) {
                throw new OnlyConsoleAllowedException(ctx);
            }
            return (ConsoleSender) minestomSource.origin();
        });

        config.registerSourceResolver(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException(ctx);
            }
            return source.as(Player.class);
        });

        config.registerSourceResolver(PlayerRef.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException(ctx);
            }
            return source.asPlayerRef();
        });
    }

    private void registerDefaultParamTypes() {
        config.registerParamType(Location.class,new ParameterLocation());
        config.registerParamType(PlayerRef.class, new ParameterPlayer());
        config.registerParamType(World.class, new ParameterWorld());
    }

    /**
     * Registers exception handlers for common Minestom command scenarios.
     * This provides user-friendly error messages for various error conditions.
     */
    private void addThrowableHandlers() {
        config.setThrowableResolver(OnlyPlayerAllowedException.class, (ex, context)-> {
            context.source().error("Only players can do this!");
        });

        // Enhanced exception handling similar to Velocity
        config.setThrowableResolver(OnlyConsoleAllowedException.class, (ex, context)-> {
            context.source().error("Only console can do this!");
        });

        config.setThrowableResolver(
                UnknownPlayerException.class, (exception, context) ->
                        context.source().origin()
                                .sendMessage(
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
    }



    @Override
    public @NotNull HytaleImperat build() {
        return new HytaleImperat(plugin, config);
    }
}
