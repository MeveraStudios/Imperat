package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;

import java.util.Objects;

/**
 * Represents a final, immutable entry for a single executable command in the help system.
 * <p>
 * This class encapsulates a {@link CommandPathway} that represents an executable command usage.
 * It serves as a reliable data model for displaying help information.
 *
 * @param <S> The type of {@link CommandSource} from which the command was executed.
 */
public final class HelpEntry<S extends CommandSource> {

    private final @NotNull CommandPathway<S> pathway;

    /**
     * Constructs a new HelpEntry from a {@link CommandPathway}.
     *
     * @param pathway The command pathway for which to create a help entry.
     */
    HelpEntry(@NotNull CommandPathway<S> pathway) {
        this.pathway = pathway;
    }

    /**
     * Retrieves the command usage pathway associated with this help entry.
     *
     * @return The pathway of the command.
     */
    public @NotNull CommandPathway<S> getPathway() {
        return pathway;
    }

    /**
     * Compares this HelpEntry to another object for equality.
     * <p>
     * Two help entries are considered equal if their underlying pathways are equal.
     *
     * @param object The object to compare with.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof HelpEntry<?> helpEntry)) {
            return false;
        }
        return Objects.equals(pathway, helpEntry.pathway);
    }

    /**
     * Computes the hash code for this HelpEntry based on its pathway.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(pathway);
    }
}
