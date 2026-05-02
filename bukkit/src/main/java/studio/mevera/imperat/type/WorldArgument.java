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

import java.util.List;

/**
 * Server-side resolver for {@link World} parameters. Used on the legacy
 * Spigot/Bukkit/Paper command-map backend (Paper-modern installs a
 * Brigadier-native variant via {@code PaperArgumentMappings}).
 *
 * <p>Resolution: looks up the world by exact name via {@link Bukkit#getWorld(String)};
 * throws {@link BukkitResponseKey#UNKNOWN_WORLD} on miss. Suggestions are the
 * names of every loaded world.</p>
 */
public class WorldArgument extends SimpleArgumentType<BukkitCommandSource, World> {

    private final WorldSuggestionProvider SUGGESTION_RESOLVER = new WorldSuggestionProvider();

    public WorldArgument() {
        super();
    }

    @Override
    public World parse(@NotNull CommandContext<BukkitCommandSource> context, @NonNull Argument<BukkitCommandSource> argument, @NotNull String input)
            throws CommandException {
        World world = Bukkit.getWorld(input);
        if (world == null) {
            throw new ArgumentParseException(BukkitResponseKey.UNKNOWN_WORLD, input);
        }
        return world;
    }

    @Override
    public SuggestionProvider<BukkitCommandSource> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    private final static class WorldSuggestionProvider implements SuggestionProvider<BukkitCommandSource> {

        @Override
        public List<String> provide(SuggestionContext<BukkitCommandSource> context, Argument<BukkitCommandSource> argument) {
            return Bukkit.getWorlds().stream().map(World::getName).toList();
        }
    }
}
