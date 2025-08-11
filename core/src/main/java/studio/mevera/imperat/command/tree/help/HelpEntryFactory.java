package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.context.Source;

/**
 * Factory interface for creating {@link HelpEntry} instances from parameter nodes.
 * Allows customization of how help entries are constructed from the command tree.
 * 
 * @param <S> the source type
 * @author Mqzen
 */
public interface HelpEntryFactory<S extends Source> {
    
    /**
     * Creates a help entry from the given parameter node.
     * 
     * @param node the parameter node to convert into a help entry
     * @return a new help entry representing the node
     * @throws IllegalArgumentException if the node cannot be converted to a help entry
     */
    HelpEntry<S> createEntry(@NotNull ParameterNode<S, ?> node);
    
    static <S extends Source> HelpEntryFactory<S> defaultFactory() {
        return HelpEntry::new;
    }
}