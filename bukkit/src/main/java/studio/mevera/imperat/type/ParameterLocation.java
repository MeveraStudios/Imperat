package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidLocationFormatException;
import studio.mevera.imperat.exception.UnknownWorldException;
import studio.mevera.imperat.util.TypeUtility;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class ParameterLocation extends BaseParameterType<BukkitSource, Location> {

    private final static String SINGLE_STRING_SEPARATOR = ";";
    private final static String SELF_LOCATION_SYMBOL = "~";

    private final ParameterType<BukkitSource, Double> doubleParser;

    public ParameterLocation() {
        super();
        doubleParser = ParameterTypes.numeric(Double.class);
    }

    @Override
    public @Nullable Location resolve(@NotNull ExecutionContext<BukkitSource> context, @NotNull CommandInputStream<BukkitSource> stream, @NotNull String input) throws
            CommandException {
        try {
            String currentRaw = stream.currentRaw().orElseThrow();
            return locFromStr(context, stream, currentRaw);
        } catch (Exception ex) {
            World world;
            String currentRaw = stream.currentRaw().orElseThrow();

            if (!TypeUtility.isNumber(currentRaw) && !currentRaw.equals(SELF_LOCATION_SYMBOL) && Bukkit.getWorld(currentRaw) != null) {
                world = Bukkit.getWorld(currentRaw);
                stream.skipRaw();
            } else if (context.source().isConsole()) {
                var worlds = Bukkit.getWorlds();
                if (worlds.isEmpty()) {
                    throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.NO_WORLDS_AVAILABLE, context);
                }
                world = Bukkit.getWorlds().get(0);
            } else {
                world = context.source().asPlayer().getWorld();
            }

            ParameterType<BukkitSource, Double> doubleParser = (ParameterType<BukkitSource, Double>) context.imperatConfig().getParameterType(Double.class);
            if (doubleParser == null) {
                throw new IllegalArgumentException("Failed to find a parser for type '" + Double.class.getTypeName() + "'");
            }

            Location playerLocation = null;
            if (!context.source().isConsole()) {
                playerLocation = context.source().asPlayer().getLocation();
            }

            String inputX = stream.readInput();
            Double x;
            if (inputX.equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, inputX, null, null, null, null, context);
                }
                x = playerLocation.getX();
            } else {
                x = doubleParser.resolve(context, stream, inputX);
                if (x == null) {
                    throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.INVALID_X_COORDINATE, inputX, null, null, null, null, context);
                }
            }
            stream.skipRaw();

            String inputY = stream.readInput();
            Double y;
            if (inputY.equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, inputY, null, null, null, context);
                }
                y = playerLocation.getY();
            } else {
                y = doubleParser.resolve(context, stream, inputY);
                if (y == null) {
                    throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.INVALID_Y_COORDINATE, null, inputY, null, null, null, context);
                }
            }
            stream.skipRaw();

            String inputZ = stream.readInput();
            Double z;
            if (inputZ.equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, null, inputZ, null, null, context);
                }
                z = playerLocation.getZ();
            } else {
                z = doubleParser.resolve(context, stream, inputZ);
                if (z == null) {
                    throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.INVALID_Z_COORDINATE, null, null, inputZ, null, null, context);
                }
            }

            float pitch = 0.0f;
            float yaw = 0.0f;

            if (stream.hasFinished()) {
                return createLocation(world, x, y, z, yaw, pitch);
            }

            stream.skipRaw();
            if (!stream.hasFinished()) {
                String inputYaw = stream.readInput();
                if (inputYaw.equals(SELF_LOCATION_SYMBOL)) {
                    if (playerLocation == null) {
                        throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, null, null, null, inputYaw, context);
                    }
                    yaw = playerLocation.getYaw();
                } else {
                    Double yawDouble = doubleParser.resolve(context, stream, inputYaw);
                    if (yawDouble == null) {
                        throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.INVALID_YAW_COORDINATE, null, null, null, null, inputYaw, context);
                    }
                    yaw = (float) yawDouble.doubleValue();
                }
                stream.skipRaw();

                if (!stream.hasFinished()) {
                    String inputPitch = stream.readInput();
                    if (inputPitch.equals(SELF_LOCATION_SYMBOL)) {
                        if (playerLocation == null) {
                            throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, null, null, inputPitch, null, context);
                        }
                        pitch = playerLocation.getPitch();
                    } else {
                        Double pitchDouble = doubleParser.resolve(context, stream, inputPitch);
                        if (pitchDouble == null) {
                            throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.INVALID_PITCH_COORDINATE, null, null, null, inputPitch, null, context);
                        }
                        pitch = (float) pitchDouble.doubleValue();
                    }
                }
            }

            return createLocation(world, x, y, z, yaw, pitch);
        }
    }

    private @NotNull Location locFromStr(ExecutionContext<BukkitSource> context, CommandInputStream<BukkitSource> stream, String currentRaw) throws
            CommandException {
        String[] split = currentRaw.split(SINGLE_STRING_SEPARATOR);
        if (split.length < 4) {
            throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.WRONG_FORMAT, context);
        }

        World world = Bukkit.getWorld(split[0]);
        if (world == null) {
            throw new UnknownWorldException(split[0]);
        }

        Location playerLocation = null;
        if (!context.source().isConsole()) {
            playerLocation = context.source().asPlayer().getLocation();
        }

        double x;
        if (split[1].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, split[1], null, null, null, null, context);
            }
            x = playerLocation.getX();
        } else {
            x = Objects.requireNonNull(doubleParser.resolve(context, stream, split[1]));
        }

        double y;
        if (split[2].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, split[2], null, null, null, context);
            }
            y = playerLocation.getY();
        } else {
            y = Objects.requireNonNull(doubleParser.resolve(context, stream, split[2]));
        }

        double z;
        if (split[3].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, null, split[3], null, null, context);
            }
            z = playerLocation.getZ();
        } else {
            z = Objects.requireNonNull(doubleParser.resolve(context, stream, split[3]));
        }

        float yaw = 0.0f;
        float pitch = 0.0f;

        if (split.length > 4) {
            if (split[4].equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, null, null, null, split[4], context);
                }
                yaw = playerLocation.getYaw();
            } else {
                yaw = (float) Objects.requireNonNull(doubleParser.resolve(context, stream, split[4])).doubleValue();
            }
        }

        if (split.length > 5) {
            if (split[5].equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.SELF_LOCATION_NOT_AVAILABLE, null, null, null, split[5], null, context);
                }
                pitch = playerLocation.getPitch();
            } else {
                pitch = (float) Objects.requireNonNull(doubleParser.resolve(context, stream, split[5])).doubleValue();
            }
        }

        return createLocation(world, x, y, z, yaw, pitch);
    }

    private Location createLocation(World world, double x, double y, double z, float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public boolean isGreedy(CommandParameter<BukkitSource> parameter) {
        return true;
    }
}