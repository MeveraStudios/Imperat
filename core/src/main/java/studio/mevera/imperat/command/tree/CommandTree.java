package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.List;
import java.util.Set;

/**
 * Represents a tree structure for commands, providing methods for parsing, matching,
 * and tab-completion functionality within a command framework.
 *
 * @param <S> the type of source that commands and nodes operate on, must extend {@link Source}
 */
public interface CommandTree<S extends Source> {

    static <S extends Source> CommandTree<S> create(ImperatConfig<S> imperatConfig, Command<S> command) {
        return new StandardCommandTree<>(imperatConfig, command);
    }

    /**
     * Gets the root command of this command tree.
     *
     * @return the root command, never null
     */
    @NotNull Command<S> root();

    /**
     * Gets the root node of this command tree.
     *
     * @return the root command node, never null
     */
    @NotNull LiteralCommandNode<S> rootNode();

    /**
     * Gets the unique versioned tree.
     * @return the unique versioned tree.
     */
    @NotNull LiteralCommandNode<S> uniqueVersionedTree();

    /**
     * Gets the unflagged unique versioned tree, which excludes flag nodes.
     * @return the unflagged unique versioned tree.
     */
    @NotNull LiteralCommandNode<S> unflaggedUniqueVersionedTree();

    /**
     * The number of nodes cached in this {@link CommandTree}.
     * @return the number of nodes representing the size
     * of this N-ary tree
     */
    int size();

    /**
     * The size of the unique versioned tree
     * @see #uniqueVersionedTree()
     * @return The size of the unique versioned tree.
     */
    int uniqueSize();

    /**
     * The size of the unflagged unique versioned tree, which excludes flag nodes.
     * @see #unflaggedUniqueVersionedTree()
     * @return The size of the unflagged unique versioned tree.
     */
    int unflaggedUniqueSize();

    /**
     * Parses the given command usage and updates the command tree accordingly.
     *
     * @param usage the command usage to parse, must not be null
     */
    void parseUsage(@NotNull CommandPathway<S> usage);

    void parseSubTree(@NotNull CommandTree<S> subTree);

    default void parseSubCommand(@NotNull Command<S> subCommand) {
        parseSubTree(subCommand.tree());
    }

    /**
     * Matches the given input against this command tree and returns a dispatch context.
     *
     * @param context The context to match against
     * @param input  the argument input to match against
     * @return a command dispatch context containing matching results, never null
     */
    @NotNull CommandPathSearch<S> contextMatch(Context<S> context, @NotNull ArgumentInput input);

    /**
     * Generates tab-completion suggestions based on the current command context.
     *
     * @param context the suggestion context
     * @return a list of tab-completion suggestions, never null
     */
    @NotNull List<String> tabComplete(
            @NotNull SuggestionContext<S> context
    );

    /**
     * Queries the help system to retrieve a set of help entries that match the specified criteria.
     *
     * <p>This method searches through available help entries and returns those that satisfy
     * the conditions defined in the provided {@link HelpQuery}. The query can include filters
     * such as search terms, categories, permissions, or other criteria depending on the
     * implementation.</p>
     *
     * @param query the help query containing search criteria and filters; must not be null
     * @return a set of {@link HelpEntry} objects that match the query criteria;
     * returns an empty set if no matches are found; never returns null
     * @see HelpQuery
     * @see HelpEntry
     * @since 2.0.0
     */
    HelpEntryList<S> queryHelp(
            @NotNull HelpQuery<S> query
    );

    /**
     * Collects the closest usages to a context, this traverses the whole {@link  CommandTree}
     * from the beginning , visiting every branch/chain possible.
     * This will lead to collecting the closest usages in-order by how close they are to your input.
     *
     * @param context the context containing the details of an input.
     * @return A set of the closest usages to a {@link Context}
     */
    Set<CommandPathway<S>> getClosestUsages(Context<S> context);

}
