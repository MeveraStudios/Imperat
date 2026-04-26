package studio.mevera.imperat.type;

import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.HytaleCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypes;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.command.arguments.type.GreedyArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidLocationFormatException;
import studio.mevera.imperat.exception.UnknownWorldException;

import java.util.Objects;

public class LocationArgument extends GreedyArgumentType<HytaleCommandSource, Location> {

    private final static String SINGLE_STRING_SEPARATOR = ";";
    private final static String SELF_LOCATION_SYMBOL = "~";

    private final ArgumentType<HytaleCommandSource, Double> doubleParser;

    public LocationArgument() {
        super();
        doubleParser = ArgumentTypes.numeric(Double.class);
    }

    private static World getWorldByName(String in) {
        return Universe.get().getWorld(in);
    }

    private static Location getPlayerLocation(PlayerRef playerRef) {
        return new Location(playerRef.getTransform().getPosition(), playerRef.getTransform().getRotation());
    }


    @Override
    public Location parse(@NotNull CommandContext<HytaleCommandSource> context, @NotNull Argument<HytaleCommandSource> argument,
            @NotNull String input) throws CommandException {
        return locFromStr(context, argument, input);
    }

    private @NotNull Location locFromStr(CommandContext<HytaleCommandSource> context, Argument<HytaleCommandSource> currentArg, String currentRaw)
            throws
            CommandException {
        String[] split = currentRaw.split(SINGLE_STRING_SEPARATOR);
        if (split.length < 4) {
            throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.WRONG_FORMAT);
        }

        World world = getWorldByName(split[0]);
        if (world == null) {
            throw new UnknownWorldException(split[0]);
        }

        Location playerLocation = null;
        if (!context.source().isConsole()) {
            playerLocation = getPlayerLocation(context.source().asPlayerRef());
        }

        double x;
        if (split[1].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, split[1],
                        null, null, null, null);
            }
            x = playerLocation.getPosition().getX();
        } else {
            x = Objects.requireNonNull(doubleParser.parse(context, currentArg, Cursor.single(context, split[1])));
        }

        double y;
        if (split[2].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null,
                        split[2], null, null, null);
            }
            y = playerLocation.getPosition().getY();
        } else {
            y = Objects.requireNonNull(doubleParser.parse(context, currentArg, Cursor.single(context, split[2])));
        }

        double z;
        if (split[3].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, null,
                        split[3], null, null);
            }
            z = playerLocation.getPosition().getZ();
        } else {
            z = Objects.requireNonNull(doubleParser.parse(context, currentArg, Cursor.single(context, split[3])));
        }

        float yaw = 0.0f;
        float pitch = 0.0f;

        if (split.length > 4) {
            if (split[4].equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null,
                            null, null, null, split[4]);
                }
                yaw = playerLocation.getRotation().getYaw();
            } else {
                yaw = (float) Objects.requireNonNull(doubleParser.parse(context, currentArg, Cursor.single(context, split[4]))).doubleValue();
            }
        }

        if (split.length > 5) {
            if (split[5].equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null,
                            null, null, split[5], null);
                }
                pitch = playerLocation.getRotation().getPitch();
            } else {
                pitch = (float) Objects.requireNonNull(doubleParser.parse(context, currentArg, Cursor.single(context, split[5]))).doubleValue();
            }
        }

        return createLocation(world, x, y, z, yaw, pitch);
    }

    private Location createLocation(World world, double x, double y, double z, float yaw, float pitch) {
        Location location = new Location(world == null ? null : world.getName(), x, y, z);
        location.setRotation(new Vector3f(yaw, pitch));
        return location;
    }

}