package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.command.tree.walk.ClosestPathwayFinder;
import studio.mevera.imperat.command.tree.walk.TreeHelpQuery;
import studio.mevera.imperat.command.tree.walk.TreeParser;
import studio.mevera.imperat.command.tree.walk.TreeSuggester;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.CommandException;

import java.util.ArrayList;
import java.util.List;

/**
 * N-ary tree representation of a {@link Command}. Acts as a façade over the
 * walk strategies in {@code command.tree.walk}:
 * <ul>
 *   <li>{@link TreeParser} — DFS execution + scoring + candidate admission.</li>
 *   <li>{@link TreeSuggester} — tab-completion walk reusing the same scoring.</li>
 *   <li>{@link TreeHelpQuery} — help index built from the {@link Command}
 *       hierarchy (not the parse tree).</li>
 *   <li>{@link ClosestPathwayFinder} — closest-usage resolution for syntax
 *       error reporting.</li>
 * </ul>
 *
 * <p>This class owns construction (parseUsage / parseSubTree) and delegates
 * traversal. Behaviour is identical to the previous monolithic implementation;
 * the split exists purely to bound complexity and make each concern testable
 * in isolation.</p>
 *
 * @param <S> the command source type
 */
public final class SuperCommandTree<S extends CommandSource> implements CommandTree<S> {

    final Node<S> root;
    private final TreeParser<S> parser;
    private final TreeSuggester<S> suggester;
    private final TreeHelpQuery<S> helpQuery;
    private final ClosestPathwayFinder<S> closestFinder;
    private int size;

    public SuperCommandTree(ImperatConfig<S> imperatConfig, Command<S> command) {
        this.root = new Node<>(null, command.getDefaultPathway(), command);
        this.root.addTerminalPathway(command.getDefaultPathway());
        this.size = 1;
        this.parser = new TreeParser<>(root);
        this.suggester = new TreeSuggester<>(root, imperatConfig);
        this.helpQuery = new TreeHelpQuery<>(root);
        this.closestFinder = new ClosestPathwayFinder<>(root);
    }

    private static <S extends CommandSource> int subTreeSize(CommandTree<S> tree) {
        if (tree instanceof SuperCommandTree<S> sct) {
            return sct.size;
        }
        return countNodes(tree.rootNode());
    }

    private static <S extends CommandSource> int countNodes(Node<S> node) {
        int count = 1;
        for (Node<S> child : node.children) {
            count += countNodes(child);
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Tree construction
    // ------------------------------------------------------------------

    @Override
    public @NotNull Node<S> rootNode() {
        return root;
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * Walks the pathway's PERSONAL arguments (excluding inherited flags), creating
     * or reusing child nodes keyed by argument name. Optional arguments are
     * accumulated onto the preceding required node; trailing optionals attach to
     * the last required node reached.
     */
    @Override
    public void parseUsage(@NotNull CommandPathway<S> usage) {
        Node<S> current = root;
        List<Argument<S>> pendingOptionals = new ArrayList<>();

        for (Argument<S> arg : usage.getArguments()) {
            if (arg.isFlag()) {
                continue; // flags belong to the pathway's FlagExtractor
            }
            if (arg.isOptional()) {
                pendingOptionals.add(arg);
                continue;
            }
            attachOptionals(current, pendingOptionals);
            pendingOptionals.clear();

            Node<S> child = current.children.stream().filter((n) -> n.main.getName().equals(arg.getName())).findFirst().orElse(null);
            if (child == null) {
                child = new Node<>(current, usage, arg);
                current.children.add(child);
                size++;
            }
            current = child;
        }
        attachOptionals(current, pendingOptionals);
        current.addTerminalPathway(usage);
    }

    private void attachOptionals(Node<S> node, List<Argument<S>> optionals) {
        if (optionals.isEmpty()) {
            return;
        }
        for (Argument<S> opt : optionals) {
            boolean exists = node.optionals.stream()
                                     .anyMatch(o -> o.getName().equals(opt.getName()));
            if (!exists) {
                node.optionals.add(opt);
            }
        }
    }

    /**
     * Attaches the root of {@code subTree} as a child of the node inside this
     * tree whose {@code main.format()} equals {@code attachmentNode}.
     */
    @Override
    public void parseSubTree(@NotNull CommandTree<S> subTree, String attachmentNode) {
        Node<S> target;
        if (attachmentNode == null || attachmentNode.isBlank()) {
            target = root;
        } else {
            target = findByFormat(root, attachmentNode);
        }
        if (target == null) {
            throw new IllegalArgumentException(
                    "No node with format '" + attachmentNode + "' in tree for command '"
                            + root.main.format() + "'");
        }
        Node<S> subRoot = subTree.rootNode();
        target.children.add(subRoot);
        subRoot.parent = target;
        size += subTreeSize(subTree);
    }

    private @Nullable Node<S> findByFormat(Node<S> node, String format) {
        if (node.main.format().equals(format)
                    || node.optionals.stream().anyMatch(optional -> optional.format().equals(format))) {
            return node;
        }
        for (Node<S> child : node.children) {
            Node<S> found = findByFormat(child, format);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Walk delegation
    // ------------------------------------------------------------------

    @Override
    public @NotNull CommandTreeMatch<S> execute(ExecutionContext<S> context, @NotNull ArgumentInput input)
            throws CommandException {
        return parser.execute(context, input);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull SuggestionContext<S> context) {
        return suggester.tabComplete(context);
    }

    @Override
    public HelpResult<S> queryHelp(@NotNull HelpQuery<S> query) {
        return helpQuery.query(query);
    }

    @Override
    public @NotNull CommandPathway<S> getClosestPathwayToContext(CommandContext<S> context, CommandTreeMatch<S> treeMatch) {
        return closestFinder.find(context, treeMatch);
    }
}
