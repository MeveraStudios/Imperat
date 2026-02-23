package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
import studio.mevera.imperat.exception.UnknownWorldException;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.util.ArrayList;

public class WorldArgument extends HytaleArgumentType<World> {

    public WorldArgument() {
        super(World.class, ArgTypes.WORLD, UnknownWorldException::new);
    }

    @Override
    public DefaultValueProvider getDefaultValueProvider() {
        World defWorld = Universe.get().getDefaultWorld();
        if (defWorld == null) {
            return DefaultValueProvider.empty();
        }

        return DefaultValueProvider.of(defWorld.getName());
    }

    @Override
    public SuggestionProvider<HytaleSource> getSuggestionProvider() {
        return (ignored, ignoredToo) -> new ArrayList<>(Universe.get().getWorlds().keySet());
    }
}
