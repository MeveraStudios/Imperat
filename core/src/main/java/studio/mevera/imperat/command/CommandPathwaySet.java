package studio.mevera.imperat.command;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandSource;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

final class CommandPathwaySet<S extends CommandSource> implements Iterable<CommandPathway<S>> {

    private final LinkedHashSet<CommandPathway<S>> sort = new LinkedHashSet<>();

    CommandPathwaySet() {
        super();
    }

    public CommandPathwaySet<S> put(CommandPathway<S> value) {
        sort.add(value);
        return this;
    }

    public Set<CommandPathway<S>> asSortedSet() {
        return sort;
    }

    @Override
    public @NotNull Iterator<CommandPathway<S>> iterator() {
        return sort.iterator();
    }

}
