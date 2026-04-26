package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.Preconditions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

final class CommandRegistry<S extends CommandSource> {

    private final Map<String, Command<S>> commands = new HashMap<>();

    void register(@NotNull Command<S> command) {
        commands.put(command.getName().trim().toLowerCase(), command);
        for (var aliases : command.aliases()) {
            commands.put(aliases.trim().toLowerCase(), command);
        }
        for (var shortcut : command.getAllShortcuts()) {
            commands.put(shortcut.getName(), shortcut);
        }
    }

    void unregister(@NotNull String name) {
        Preconditions.notNull(name, "commandToRemove");
        Command<S> removed = commands.remove(name.trim().toLowerCase());
        if (removed != null) {
            for (var aliases : removed.aliases()) {
                commands.remove(aliases.trim().toLowerCase());
            }
        }
    }

    void clear() {
        commands.clear();
    }

    @Nullable Command<S> get(@NotNull String name) {
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

    Collection<? extends Command<S>> values() {
        return commands.values();
    }
}
