package studio.mevera.imperat.command.tree.projection;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.context.CommandSource;

import java.util.List;

/**
 * Immutable view of a single tree node for platform-integration consumers.
 * Wraps the underlying {@link Node} so consumers retain access to framework
 * semantics (permission check, original pathway, terminal pathways) while
 * also seeing the projection's enrichments — most importantly, the per-scope
 * {@link ProjectedFlag} list that replaces the legacy synthetic
 * "flag-tunnel" suggestion node.
 *
 * <p>The list shapes returned here are immutable views built once at
 * projection time. Iterate freely; do not cast back to mutable types.</p>
 *
 * @param <S> the command-source type
 */
public final class ProjectedNode<S extends CommandSource> {

    private final Node<S> source;
    private final List<ProjectedNode<S>> children;
    private final List<Argument<S>> optionalArguments;
    private final List<ProjectedFlag<S>> flags;

    ProjectedNode(@NotNull Node<S> source,
            @NotNull List<ProjectedNode<S>> children,
            @NotNull List<Argument<S>> optionalArguments,
            @NotNull List<ProjectedFlag<S>> flags) {
        this.source = source;
        this.children = List.copyOf(children);
        this.optionalArguments = List.copyOf(optionalArguments);
        this.flags = List.copyOf(flags);
    }

    /**
     * The underlying tree node. Consumers use this to call back into the
     * framework's permission machinery (e.g. {@code originalPathway()},
     * {@code getMainArgument()}) without re-implementing those checks.
     */
    public @NotNull Node<S> sourceNode() {
        return source;
    }

    public @NotNull Argument<S> mainArgument() {
        return source.getMainArgument();
    }

    public boolean isRoot() {
        return source.isRoot();
    }

    public boolean isLeaf() {
        return source.isLeaf();
    }

    public @NotNull CommandPathway<S> originalPathway() {
        return source.getOriginalPathway();
    }

    public @NotNull Iterable<CommandPathway<S>> terminalPathways() {
        return source.getTerminalPathways();
    }

    public @NotNull List<ProjectedNode<S>> children() {
        return children;
    }

    public @NotNull List<Argument<S>> optionalArguments() {
        return optionalArguments;
    }

    /**
     * Every flag reachable at this node's scope (including flags inherited
     * from owning command + parent pathway scopes), de-duplicated across
     * overlapping registrations and ordered by canonical name. Unlike the
     * legacy walk this is computed once at projection time.
     */
    public @NotNull List<ProjectedFlag<S>> flags() {
        return flags;
    }
}
