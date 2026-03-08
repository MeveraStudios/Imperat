package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;

/**
 * Factory interface for creating {@link HelpEntry} instances from command pathways.
 * Allows customization of how help entries are constructed.
 *
 * @param <S> the source type
 * @author Mqzen
 */
public interface HelpEntryFactory<S extends CommandSource> {

    static <S extends CommandSource> HelpEntryFactory<S> defaultFactory() {
        return HelpEntry::new;
    }

    /**
     * Creates a help entry from the given command pathway.
     *
     * @param pathway the command pathway to convert into a help entry
     * @return a new help entry representing the pathway
     */
    HelpEntry<S> createEntry(@NotNull CommandPathway<S> pathway);
}