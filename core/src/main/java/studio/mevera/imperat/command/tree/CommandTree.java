package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
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
     * Compute Permissions in APA(AutoPermissionAssign) mode
     * This is a method that shall take some considerable amount of resources whenever
     * an instance of {@link Command} is created using its {@link Command.Builder}.
     */
    void computePermissions();
    
    /**
     * This should fetch the command parameter's assigned permission.
     * Use this method when necessary only.
     * @throws IllegalStateException when the APA(AutoPermissionAssign) mode is NOT enabled.
     * @param commandParameter the parameter
     * @return the permission that was auto assigned for it.
     */
    @Nullable String getAutoAssignedPermission(@NotNull CommandParameter<S> commandParameter);
    
    /**
     * Matches the given input against this command tree and returns a dispatch context.
     *
     * @param source the source/sender/executor executing the command.
     * @param input  the argument input to match against
     * @return a command dispatch context containing matching results, never null
     */
    @NotNull CommandPathSearch<S> contextMatch(S source, @NotNull ArgumentInput input);
    
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
     * Collects the closest usages to a context, this traverses the whole {@link  CommandTree}
     * from the beginning , visiting every branch/chain possible.
     * This will lead to collecting the closest usages in-order by how close they are to your input.
     *
     * @param context the context containing the details of an input.
     * @return A set of the closest usages to a {@link Context}
     */
    Set<CommandUsage<S>> getClosestUsages(Context<S> context);
    
    static <S extends Source> CommandTree<S> create(ImperatConfig<S> imperatConfig, Command<S> command) {
        return new StandardCommandTree<>(imperatConfig, command);
    }
    
    static <S extends Source> CommandTree<S> parsed(ImperatConfig<S> imperatConfig, Command<S> command) {
        var tree = new StandardCommandTree<>(imperatConfig, command);
        tree.parseCommandUsages();
        return tree;
    }
    
}
