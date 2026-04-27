package studio.mevera.imperat.command.tree.walk;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.tree.CommandTreeMatch;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.command.tree.ParsedNode;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the most plausible {@link CommandPathway} for an unsuccessful or
 * partially successful match — used by syntax-error reporting and "did you
 * mean" UX. Extracted from the former {@code SuperCommandTree}; semantics are
 * unchanged.
 */
public final class ClosestPathwayFinder<S extends CommandSource> {

    private final Node<S> root;

    public ClosestPathwayFinder(@NotNull Node<S> root) {
        this.root = root;
    }

    public @NotNull CommandPathway<S> find(CommandContext<S> context, CommandTreeMatch<S> treeMatch) {
        var parsedNodesList = treeMatch.parsedNodes();
        if (parsedNodesList.isEmpty()) {
            return treeMatch.pathway() != null ? treeMatch.pathway() : root.getOriginalPathway();
        }
        ParsedNode<S> node = parsedNodesList.get(parsedNodesList.size() - 1);
        CommandPathway<S> pathway = node.getOriginalPathway();

        List<Node<S>> path = traverse(new ArrayList<>(), node.getDelegate(), context);
        return path.isEmpty() ? pathway : path.get(path.size() - 1).getOriginalPathway();
    }

    private List<Node<S>> traverse(List<Node<S>> path, Node<S> node, CommandContext<S> context) {
        if (!node.isRoot()) {
            path.add(node);
        }
        if (node.isLeaf()) {
            return path;
        }
        var topChild = node.getChildren().iterator().next();
        return traverse(path, topChild, context);
    }
}
