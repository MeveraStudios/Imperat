package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.context.Source;

import java.util.Objects;

/**
 * Represents a final, immutable entry for a single executable command in the help system.
 * <p>
 * This class encapsulates a {@link ParameterNode} that represents an executable command,
 * along with its corresponding {@link CommandUsage} pathway. It ensures that only
 * executable nodes can be used to create a help entry, making it a reliable data
 * model for displaying help information.
 *
 * @param <S> The type of {@link Source} from which the command was executed.
 */
public final class HelpEntry<S extends Source> {
    
    private final ParameterNode<S, ?> node;
    private final @NotNull CommandUsage<S> pathway;
    
    /**
     * Constructs a new HelpEntry from an executable {@link ParameterNode}.
     *
     * @param node The executable node for which to create a help entry.
     * @throws IllegalArgumentException if the provided node is not executable.
     */
    HelpEntry(ParameterNode<S, ?> node) {
        if(!node.isExecutable()) {
            throw new IllegalArgumentException("Node '" + node.format() + "' is not executable");
        }
        this.node = node;
        
        assert node.getExecutableUsage() != null;
        this.pathway = node.getExecutableUsage();
    }
    
    /**
     * Retrieves the command usage pathway associated with this help entry.
     *
     * @return The pathway of the command.
     */
    public @NotNull CommandUsage<S> getPathway() {
        return pathway;
    }
    
    /**
     * Retrieves the parameter node associated with this help entry.
     *
     * @return The parameter node.
     */
    public ParameterNode<S, ?> getNode() {
        return node;
    }
    
    /**
     * Compares this HelpEntry to another object for equality.
     * <p>
     * Two help entries are considered equal if their underlying parameter nodes are equal.
     *
     * @param object The object to compare with.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof HelpEntry<?> helpEntry)) return false;
        return Objects.equals(node, helpEntry.node);
    }
    
    /**
     * Computes the hash code for this HelpEntry based on its parameter node.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(node);
    }
}
