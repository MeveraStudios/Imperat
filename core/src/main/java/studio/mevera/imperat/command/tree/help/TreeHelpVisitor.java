package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;

/**
 * Provides help data by visiting the command tree, node by node.
 * Responsible ONLY for data retrieval and filtering, NOT rendering.
 * 
 * @param <S> the source type
 */
public interface TreeHelpVisitor<S extends Source> {
    
    /**
     * Queries help entries based on the given query parameters.
     * 
     * @param tree the command tree to query
     * @param query the query parameters
     * @return filtered help entries
     */
    HelpEntryList<S> visit(Command<S> tree, HelpQuery<S> query);
    
    static <S extends Source> TreeHelpVisitor<S> defaultProvider() {
        return new StandardTreeHelpVisitor<>();
    }
}