package studio.mevera.imperat.command.tree.walk;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.tree.CommandTreeMatch;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the most plausible {@link CommandPathway} for an unsuccessful or
 * partially successful match — used by syntax-error reporting and "did you
 * mean" UX.
 *
 * <p>For each candidate node, prefers the <em>richest</em> registered
 * pathway over {@link Node#getOriginalPathway()}. The original-pathway
 * field on a node holds whichever pathway was registered first — if a
 * later pathway converged at the same spine node and added tail
 * optionals or flags, those wouldn't show up in the original. Walking
 * {@link Node#getTerminalPathways()} and picking the one with the
 * highest argument-plus-flag count surfaces the variant the user most
 * likely intended.</p>
 */
public final class ClosestPathwayFinder<S extends CommandSource> {

    private final Node<S> root;

    public ClosestPathwayFinder(@NotNull Node<S> root) {
        this.root = root;
    }

    public @NotNull CommandPathway<S> find(CommandContext<S> context, CommandTreeMatch<S> treeMatch) {
        var parsedNodesList = treeMatch.parsedNodes();

        // The match's top-level pathway hint is only authoritative when
        // there's NO parse history to walk — once parsed nodes exist the
        // deepest delegate is more informative than the match's top-level
        // pick. Short-circuiting on a non-null {@code treeMatch.pathway()}
        // regardless of parse state regresses every closest-usage scenario
        // where the validation that throws (e.g. validatePresenceOfRequired)
        // attaches a shallow candidate-default pathway to the match
        // instead of a leaf one.
        if (parsedNodesList.isEmpty() && treeMatch.pathway() != null) {
            return treeMatch.pathway();
        }

        // Pick a starting node for the descent. With parsed nodes present
        // we resume from the deepest delegate — historical, well-tested
        // behaviour every closest-usage test relies on. When the parse
        // history is empty AND no top-level pathway hint is attached
        // (e.g. unknown subcommand under a parent that has children, where
        // the new {@code TreeParser} surfaces a synthetic failure and
        // returns an empty {@link CommandTreeMatch}), walk from root so
        // the descent still finds a registered descendant pathway. Without
        // it we'd hand back root's bare default and the user would see
        // "/parent" instead of "/parent foo".
        Node<S> startingNode = parsedNodesList.isEmpty()
                                       ? root
                                       : parsedNodesList.get(parsedNodesList.size() - 1).getDelegate();

        CommandPathway<S> fallback = richestPathway(startingNode);
        List<Node<S>> path = traverse(new ArrayList<>(), startingNode, context);
        return path.isEmpty() ? fallback : richestPathway(path.get(path.size() - 1));
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

    /**
     * Picks the pathway carrying the most argument + flag slots from the
     * node's {@link Node#getTerminalPathways() terminals}, falling back to
     * {@link Node#getOriginalPathway()} when no terminals are registered
     * (intermediate-spine nodes). Tie-broken by raw argument count, then
     * by registration order (first registered wins).
     */
    private @NotNull CommandPathway<S> richestPathway(Node<S> node) {
        List<CommandPathway<S>> terminals = node.getTerminalPathways();
        if (terminals.isEmpty()) {
            return node.getOriginalPathway();
        }
        CommandPathway<S> best = terminals.get(0);
        int bestSize = pathwaySize(best);
        for (int i = 1; i < terminals.size(); i++) {
            CommandPathway<S> candidate = terminals.get(i);
            int candidateSize = pathwaySize(candidate);
            if (candidateSize > bestSize) {
                best = candidate;
                bestSize = candidateSize;
            }
        }
        return best;
    }

    private int pathwaySize(CommandPathway<S> pathway) {
        // getArgumentsWithFlags includes positional optionals (already in
        // getArguments) plus all registered flag arguments — exactly the
        // surface formatted into the closest-usage string.
        return pathway.getArgumentsWithFlags().size();
    }
}
