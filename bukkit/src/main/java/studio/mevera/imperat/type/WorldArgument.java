package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.BukkitResponseKey;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-side resolver for {@link World} parameters. Used on the legacy
 * Spigot/Bukkit/Paper command-map backend (Paper-modern installs a
 * Brigadier-native variant via {@code PaperArgumentMappings}).
 *
 * <p>Resolution: looks up the world by exact name via {@link Bukkit#getWorld(String)};
 * throws {@link BukkitResponseKey#UNKNOWN_WORLD} on miss. Suggestions are the
 * names of every loaded world.</p>
 *
 * <h3>Cross-version safety</h3>
 *
 * <p>Modern Paper extracted {@code World#getName()} into a
 * {@code WorldInfo} superinterface. {@code javac} compiled against that
 * API emits {@code invokeinterface WorldInfo.getName} for any direct
 * call or method reference — which crashes with
 * {@code NoClassDefFoundError: org/bukkit/generator/WorldInfo} on legacy
 * Spigot 1.8.x where {@code WorldInfo} doesn't exist. The suggester
 * uses a reflective invoke so the call site never binds
 * to {@code WorldInfo} at the bytecode level. The reflective handle is
 * resolved once at class init and cached.</p>
 */
public class WorldArgument<S extends BukkitCommandSource> extends SimpleArgumentType<S, World> {

    /**
     * Cached {@code World#getName()} handle. Looked up once via
     * {@code World.class.getMethod} so the bytecode never references
     * {@code WorldInfo} directly. {@code null} on platforms where the
     * method is unavailable (defensive — should never trip on any real
     * Bukkit derivative).
     */
    private static final Method GET_NAME = lookupGetName();

    private final WorldSuggestionProvider<S> SUGGESTION_RESOLVER = new WorldSuggestionProvider<>();

    public WorldArgument() {
        super();
    }

    @Override
    public World parse(@NotNull CommandContext<S> context, @NonNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        World world = Bukkit.getWorld(input);
        if (world == null) {
            throw new ArgumentParseException(BukkitResponseKey.UNKNOWN_WORLD, input);
        }
        return world;
    }

    @Override
    public SuggestionProvider<S> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    private static Method lookupGetName() {
        try {
            return World.class.getMethod("getName");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Reflective {@code World#getName()} call. Avoids any bytecode-level
     * reference to {@code WorldInfo} that {@code javac} would otherwise
     * emit when compiled against the modern Paper API.
     */
    private static String safeGetName(World world) {
        if (GET_NAME == null) {
            return null;
        }
        try {
            return (String) GET_NAME.invoke(world);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private final static class WorldSuggestionProvider<S extends BukkitCommandSource> implements SuggestionProvider<S> {

        @Override
        public List<String> provide(SuggestionContext<S> context, Argument<S> argument) {
            List<World> worlds = Bukkit.getWorlds();
            List<String> names = new ArrayList<>(worlds.size());
            for (World world : worlds) {
                String name = safeGetName(world);
                if (name != null) {
                    names.add(name);
                }
            }
            return names;
        }
    }
}
