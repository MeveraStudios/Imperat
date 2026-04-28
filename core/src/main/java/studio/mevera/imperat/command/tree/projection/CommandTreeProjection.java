package studio.mevera.imperat.command.tree.projection;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.CommandSource;

/**
 * An immutable, platform-neutral view of a command tree, built once per
 * registration and consumed by platform-integration layers (Brigadier, JDA
 * slash commands, etc.) instead of having each layer re-walk the runtime
 * {@code Node<S>} tree itself.
 *
 * <p>The projection is intentionally a structural image of the parse tree,
 * with two enrichments that platform translators previously had to derive
 * themselves:
 * <ul>
 *   <li><b>Flags promoted to first-class entries.</b> The runtime tree stores
 *       flags off-band (on the pathway's flag extractor); the projection
 *       surfaces them as {@link ProjectedFlag} entries on each
 *       {@link ProjectedNode}, ordered by name and de-duplicated across
 *       overlapping pathway scopes.</li>
 *   <li><b>Per-node pathway scopes pre-resolved.</b> The set of pathways
 *       reachable through a given node is computed once at projection time
 *       (mirroring the legacy {@code resolveFlagScopePathways} walk) so
 *       consumers do not re-run that computation per platform call.</li>
 * </ul>
 *
 * <p>The projection holds <em>references</em> to the underlying
 * {@link Command}, {@link studio.mevera.imperat.command.tree.Node}, and
 * {@link studio.mevera.imperat.command.arguments.Argument} objects — not
 * copies — so it stays cheap to build and keeps consumer code able to fall
 * through to the underlying objects when it needs framework-side semantics
 * (permission checks, suggestion provider resolution, etc.).</p>
 *
 * @param <S> the command-source type
 */
public final class CommandTreeProjection<S extends CommandSource> {

    private final Command<S> command;
    private final ProjectedNode<S> root;

    private CommandTreeProjection(Command<S> command, ProjectedNode<S> root) {
        this.command = command;
        this.root = root;
    }

    /**
     * Project the given command's tree once. The returned projection is
     * immutable; rebuild it after mutating the command's pathways or
     * subcommands.
     */
    public static <S extends CommandSource> @NotNull CommandTreeProjection<S> of(@NotNull Command<S> command) {
        ProjectedNode<S> root = ProjectionBuilder.build(command);
        return new CommandTreeProjection<>(command, root);
    }

    public @NotNull Command<S> command() {
        return command;
    }

    public @NotNull ProjectedNode<S> root() {
        return root;
    }
}
