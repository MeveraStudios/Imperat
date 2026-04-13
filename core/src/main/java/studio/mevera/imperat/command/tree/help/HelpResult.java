package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandSource;

import java.util.Iterator;
import java.util.List;

/**
 * Immutable result of a help query.
 *
 * @param <S> the source type
 */
public final class HelpResult<S extends CommandSource> implements Iterable<HelpEntry<S>> {

    private static final HelpResult<?> EMPTY = new HelpResult<>(List.of());

    private final @NotNull List<HelpEntry<S>> entries;

    private HelpResult(@NotNull List<HelpEntry<S>> entries) {
        this.entries = entries;
    }

    public static <S extends CommandSource> @NotNull HelpResult<S> empty() {
        return (HelpResult<S>) EMPTY;
    }

    public static <S extends CommandSource> @NotNull HelpResult<S> copyOf(@NotNull List<HelpEntry<S>> entries) {
        if (entries.isEmpty()) {
            return empty();
        }
        return new HelpResult<>(List.copyOf(entries));
    }

    public @NotNull List<HelpEntry<S>> entries() {
        return entries;
    }

    public HelpEntry<S> get(int index) {
        return entries.get(index);
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public @NotNull Iterator<HelpEntry<S>> iterator() {
        return entries.iterator();
    }
}
