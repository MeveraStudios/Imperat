package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandRegistry;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.Preconditions;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * In-memory {@link CommandRegistry} keyed by lowercase command name, alias,
 * and shortcut. Default implementation used by {@code BaseImperat} when the
 * embedder does not supply a custom registry.
 *
 * <p>Not thread-safe; the legacy implementation also assumed single-threaded
 * registration and this preserves that contract. Embedders that need
 * concurrent registration should supply their own registry.</p>
 */
final class MapCommandRegistry<S extends CommandSource> implements CommandRegistry<S> {

    private final Map<String, Command<S>> commands = new HashMap<>();

    @Override
    public void register(@NotNull Command<S> command) {
        commands.put(command.getName().trim().toLowerCase(), command);
        for (var aliases : command.aliases()) {
            commands.put(aliases.trim().toLowerCase(), command);
        }
        for (var shortcut : command.getAllShortcuts()) {
            commands.put(shortcut.getName(), shortcut);
        }
    }

    @Override
    public void unregister(@NotNull String name) {
        Preconditions.notNull(name, "commandToRemove");
        Command<S> removed = commands.remove(name.trim().toLowerCase());
        if (removed != null) {
            for (var aliases : removed.aliases()) {
                commands.remove(aliases.trim().toLowerCase());
            }
        }
    }

    @Override
    public void clear() {
        commands.clear();
    }

    @Override
    public @Nullable Command<S> get(@NotNull String name) {
        final String cmdName = name.toLowerCase();
        final Command<S> result = commands.get(cmdName);
        if (result != null) {
            return result;
        }
        for (Command<S> headCommand : commands.values()) {
            if (headCommand.hasName(cmdName)) {
                return headCommand;
            }
        }
        return null;
    }

    @Override
    public @NotNull Collection<? extends Command<S>> values() {
        // Multiple keys (canonical + aliases) point to the same command;
        // collapse to one entry. Insertion-ordered for stable visualization.
        return new LinkedHashSet<>(commands.values());
    }
}
