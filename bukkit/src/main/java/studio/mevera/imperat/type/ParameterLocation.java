package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.InvalidLocationFormatException;
import studio.mevera.imperat.exception.UnknownWorldException;
import studio.mevera.imperat.util.TypeUtility;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class ParameterLocation extends BaseParameterType<BukkitSource, Location> {

    private final static String SINGLE_STRING_SEPARATOR = ";";

    private final ParameterType<BukkitSource, Double> doubleParser;
    public ParameterLocation() {
        super();
        doubleParser = ParameterTypes.numeric(Double.class);
    }

    @Override
    public @Nullable Location resolve(@NotNull ExecutionContext<BukkitSource> context, @NotNull CommandInputStream<BukkitSource> stream, @NotNull String input) throws ImperatException {
        try {
            String currentRaw = stream.currentRaw().orElseThrow();
            return locFromStr(context, stream, currentRaw);
        }catch (Exception ex) {

            World world;
            String currentRaw = stream.currentRaw().orElseThrow();
            if(!TypeUtility.isNumber(currentRaw) && Bukkit.getWorld(currentRaw) != null) {
                world = Bukkit.getWorld(currentRaw);
                stream.skipRaw();
            }else if(context.source().isConsole()) {
                var worlds = Bukkit.getWorlds();
                if(worlds.isEmpty()) {
                    //worlds may be empty.
                    throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.NO_WORLDS_AVAILABLE, context);
                }
                world = Bukkit.getWorlds().get(0);
            }else {
                world = context.source().asPlayer().getWorld();
            }

            ParameterType<BukkitSource, Double> doubleParser = (ParameterType<BukkitSource, Double>) context.imperatConfig().getParameterType(Double.class);
            if(doubleParser == null) {
                throw new IllegalArgumentException("Failed to find a parser for type '" + Double.class.getTypeName() + "'");
            }

            String inputX = stream.readInput();
            Double x = doubleParser.resolve(context, stream, inputX);
            if(x == null ) throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.INVALID_X_COORDINATE, inputX, null, null, context);
            stream.skipRaw();

            String inputY = stream.readInput();
            Double y = doubleParser.resolve(context, stream, inputY);
            if(y == null ) throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.INVALID_Y_COORDINATE, null, inputY, null, context);
            stream.skipRaw();

            String inputZ = stream.readInput();
            Double z = doubleParser.resolve(context, stream, inputZ);
            if(z == null) throw new InvalidLocationFormatException(input, InvalidLocationFormatException.Reason.INVALID_Z_COORDINATE, null, null, inputZ, context);

            return new Location(world, x, y, z);
        }

    }

    private @NotNull Location locFromStr(ExecutionContext<BukkitSource> context, CommandInputStream<BukkitSource> stream, String currentRaw) throws ImperatException {
        String[] split = currentRaw.split(SINGLE_STRING_SEPARATOR);
        if(split.length < 4) {
            throw new InvalidLocationFormatException(currentRaw, InvalidLocationFormatException.Reason.WRONG_FORMAT, context);
        }

        World world = Bukkit.getWorld(split[0]);
        if(world == null) {
            throw new UnknownWorldException(split[0], context);
        }

        Double x = Objects.requireNonNull(doubleParser.resolve(context, stream, split[1]));
        Double y = Objects.requireNonNull(doubleParser.resolve(context, stream, split[2]));
        Double z = Objects.requireNonNull(doubleParser.resolve(context, stream, split[3]));

        return new Location(world, x, y, z);
    }

}
