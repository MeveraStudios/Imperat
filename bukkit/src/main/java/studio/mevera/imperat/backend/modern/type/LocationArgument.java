package studio.mevera.imperat.backend.modern.type;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypes;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.command.arguments.type.GreedyArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.BukkitResponseKey;

public class LocationArgument extends GreedyArgumentType<BukkitCommandSource, Location> {

    private static final String SINGLE_STRING_SEPARATOR = ";";
    private static final String SELF_LOCATION_SYMBOL = "~";

    private final ArgumentType<BukkitCommandSource, Double> doubleParser;

    public LocationArgument() {
        super();
        doubleParser = ArgumentTypes.numeric(Double.class);
    }

    @Override
    public Location parse(@NotNull CommandContext<BukkitCommandSource> context,
            @NonNull Argument<BukkitCommandSource> argument,
                          @NotNull String input) throws CommandException {
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

        double x = resolveCoord(split[1], playerLocation == null ? null : playerLocation.getX(),
                input, "INVALID_X_COORDINATE", context, argument, true, false, false, false, false);
        double y = resolveCoord(split[2], playerLocation == null ? null : playerLocation.getY(),
                input, "INVALID_Y_COORDINATE", context, argument, false, true, false, false, false);
        double z = resolveCoord(split[3], playerLocation == null ? null : playerLocation.getZ(),
                input, "INVALID_Z_COORDINATE", context, argument, false, false, true, false, false);

        float yaw = 0.0f;
        float pitch = 0.0f;

        if (split.length > 4) {
            yaw = (float) resolveCoord(split[4], playerLocation == null ? null : (double) playerLocation.getYaw(),
                    input, "INVALID_YAW_COORDINATE", context, argument, false, false, false, false, true);
        }
        if (split.length > 5) {
            pitch = (float) resolveCoord(split[5], playerLocation == null ? null : (double) playerLocation.getPitch(),
                    input, "INVALID_PITCH_COORDINATE", context, argument, false, false, false, true, false);
        }

        return new Location(world, x, y, z, yaw, pitch);
    }

    private double resolveCoord(String token, Double selfValue, String input, String invalidReason,
            CommandContext<BukkitCommandSource> context, Argument<BukkitCommandSource> argument,
                                boolean isX, boolean isY, boolean isZ, boolean isPitch, boolean isYaw) throws CommandException {
        if (SELF_LOCATION_SYMBOL.equals(token)) {
            if (selfValue == null) {
                throw createLocationException(input, "SELF_LOCATION_NOT_AVAILABLE",
                        isX ? token : null, isY ? token : null, isZ ? token : null,
                        isPitch ? token : null, isYaw ? token : null);
            }
            return selfValue;
        }
        return doubleParser.parse(context, argument, Cursor.single(context, token));
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
}
