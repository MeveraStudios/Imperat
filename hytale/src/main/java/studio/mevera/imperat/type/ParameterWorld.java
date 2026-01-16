package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.UnknownWorldException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import java.util.ArrayList;

public class ParameterWorld extends BaseParameterType<HytaleSource, World> {

    @Override
    public @Nullable World resolve(
            @NotNull ExecutionContext<HytaleSource> context,
            @NotNull CommandInputStream<HytaleSource> inputStream,
            @NotNull String input
    ) throws ImperatException {
        World world = Universe.get().getWorld(input);
        if(world == null) {
            throw new UnknownWorldException(input, context);
        }
        return world;
    }

    @Override
    public OptionalValueSupplier supplyDefaultValue() {
        World defWorld = Universe.get().getDefaultWorld();
        if (defWorld == null) {
            return OptionalValueSupplier.empty();
        }

        return OptionalValueSupplier.of(defWorld.getName());
    }

    @Override
    public SuggestionResolver<HytaleSource> getSuggestionResolver() {
        return (_, _)->
                new ArrayList<>(Universe.get().getWorlds().keySet());
    }
}
