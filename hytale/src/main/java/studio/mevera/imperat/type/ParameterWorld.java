package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.exception.UnknownWorldException;
import studio.mevera.imperat.resolvers.SuggestionResolver;

import java.util.ArrayList;

public class ParameterWorld extends HytaleParameterType<World> {

    public ParameterWorld() {
        super(World.class, ArgTypes.WORLD, UnknownWorldException::new);
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
        return (ignored, ignoredToo) -> new ArrayList<>(Universe.get().getWorlds().keySet());
    }
}
