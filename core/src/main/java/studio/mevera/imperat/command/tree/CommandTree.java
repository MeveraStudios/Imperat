package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.List;

/**
 * Represents a tree structure for commands, providing methods for parsing, matching,
 * and tab-completion functionality within a command framework.
 *
 * @param <S> the type of source that commands and nodes operate on, must extend {@link Source}
 */
public interface CommandTree<S extends Source> {
    
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
    @NotNull CommandNode<S> rootNode();
    
    /**
     * The number of nodes cached in this {@link CommandTree}.
     * @return the number of nodes representing the size
     * of this N-ary tree
     */
    int size();
    
    /**
     * Parses the given command usage and updates the command tree accordingly.
     *
     * @param usage the command usage to parse, must not be null
     */
    void parseUsage(@NotNull CommandUsage<S> usage);
    
    /**
     * Matches the given input against this command tree and returns a dispatch context.
     *
     * @param input the argument input to match against
     * @return a command dispatch context containing matching results, never null
     */
    @NotNull CommandPathSearch<S> contextMatch(@NotNull ArgumentInput input);
    
    /**
     * Generates tab-completion suggestions based on the current command context.
     *
     * @param context the suggestion context
     * @return a list of tab-completion suggestions, never null
     */
    @NotNull List<String> tabComplete(
            @NotNull SuggestionContext<S> context
    );
    
    static <S extends Source> CommandTree<S> create(ImperatConfig<S> imperatConfig, Command<S> command) {
        return new StandardCommandTree<>(imperatConfig, command);
    }
    
    static <S extends Source> CommandTree<S> parsed(ImperatConfig<S> imperatConfig, Command<S> command) {
        var tree = new StandardCommandTree<>(imperatConfig, command);
        tree.parseCommandUsages();
        return tree;
    }
    
}
