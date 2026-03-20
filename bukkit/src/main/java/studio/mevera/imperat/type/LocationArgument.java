package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypes;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.BukkitResponseKey;

public class LocationArgument extends ArgumentType<BukkitCommandSource, Location> {

    private final static String SINGLE_STRING_SEPARATOR = ";";
    private final static String SELF_LOCATION_SYMBOL = "~";

    private final ArgumentType<BukkitCommandSource, Double> doubleParser;

    public LocationArgument() {
        super();
        doubleParser = ArgumentTypes.numeric(Double.class);
    }

    @Override
    public Location parse(@NotNull CommandContext<BukkitCommandSource> context, @NotNull String input) throws CommandException {
        // Parse from a single string (semicolon-separated)
        String[] split = input.split(SINGLE_STRING_SEPARATOR);
        if (split.length < 4) {
            throw createLocationException(input, "WRONG_FORMAT", null, null, null, null, null);
        }

        World world = Bukkit.getWorld(split[0]);
        if (world == null) {
            throw ResponseException.of(BukkitResponseKey.UNKNOWN_WORLD)
                          .withPlaceholder("name", split[0]);
        }

        Location playerLocation = null;
        if (!context.source().isConsole()) {
            playerLocation = context.source().asPlayer().getLocation();
        }

        double x;
        if (split[1].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw createLocationException(input, "SELF_LOCATION_NOT_AVAILABLE", split[1], null, null, null, null);
            }
            x = playerLocation.getX();
        } else {
            x = doubleParser.parse(context, split[1]);
        }

        double y;
        if (split[2].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw createLocationException(input, "SELF_LOCATION_NOT_AVAILABLE", null, split[2], null, null, null);
            }
            y = playerLocation.getY();
        } else {
            y = doubleParser.parse(context, split[2]);
        }

        double z;
        if (split[3].equals(SELF_LOCATION_SYMBOL)) {
            if (playerLocation == null) {
                throw createLocationException(input, "SELF_LOCATION_NOT_AVAILABLE", null, null, split[3], null, null);
            }
            z = playerLocation.getZ();
        } else {
            z = doubleParser.parse(context, split[3]);
        }

        float yaw = 0.0f;
        float pitch = 0.0f;

        if (split.length > 4) {
            if (split[4].equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw createLocationException(input, "SELF_LOCATION_NOT_AVAILABLE", null, null, null, null, split[4]);
                }
                yaw = playerLocation.getYaw();
            } else {
                yaw = (float) doubleParser.parse(context, split[4]).doubleValue();
            }
        }

        if (split.length > 5) {
            if (split[5].equals(SELF_LOCATION_SYMBOL)) {
                if (playerLocation == null) {
                    throw createLocationException(input, "SELF_LOCATION_NOT_AVAILABLE", null, null, null, split[5], null);
                }
                pitch = playerLocation.getPitch();
            } else {
                pitch = (float) doubleParser.parse(context, split[5]).doubleValue();
            }
        }

        return createLocation(world, x, y, z, yaw, pitch);
    }

    @Override
    public Location parse(@NotNull ExecutionContext<BukkitCommandSource> context, @NotNull Cursor<BukkitCommandSource> cursor)
            throws CommandException {
        String currentRaw = cursor.currentRaw().orElse("");
        return parse(context, currentRaw);
    }

    private Location createLocation(World world, double x, double y, double z, float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    private ResponseException createLocationException(String input, String reason, String inputX, String inputY, String inputZ, String inputPitch,
            String inputYaw) {
        String message = switch (reason) {
            case "INVALID_X_COORDINATE" -> "Invalid X coordinate '" + inputX + "'";
            case "INVALID_Y_COORDINATE" -> "Invalid Y coordinate '" + inputY + "'";
            case "INVALID_Z_COORDINATE" -> "Invalid Z coordinate '" + inputZ + "'";
            case "INVALID_YAW_COORDINATE" -> "Invalid Yaw coordinate '" + inputYaw + "'";
            case "INVALID_PITCH_COORDINATE" -> "Invalid Pitch coordinate '" + inputPitch + "'";
            case "NO_WORLDS_AVAILABLE" -> "Failed to fetch the world of the given location";
            case "WRONG_FORMAT" -> "Wrong location format!";
            case "SELF_LOCATION_NOT_AVAILABLE" -> "Self location not available";
            default -> "Unknown location error";
        };

        return ResponseException.of(BukkitResponseKey.INVALID_LOCATION)
                       .withPlaceholder("input", input)
                       .withPlaceholder("cause", message)
                       .withPlaceholder("reason", reason)
                       .withPlaceholder("inputX", inputX != null ? inputX : "")
                       .withPlaceholder("inputY", inputY != null ? inputY : "")
                       .withPlaceholder("inputZ", inputZ != null ? inputZ : "")
                       .withPlaceholder("inputPitch", inputPitch != null ? inputPitch : "")
                       .withPlaceholder("inputYaw", inputYaw != null ? inputYaw : "");
    }

    @Override
    public boolean isGreedy(Argument<BukkitCommandSource> parameter) {
        return true;
    }
}
