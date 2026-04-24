package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.ImperatDebugger;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public final class CommandTreeVisualizer<S extends CommandSource> {

    private final @Nullable CommandTree<S> tree;
    private final boolean showNodeTypes;

    CommandTreeVisualizer(@Nullable CommandTree<S> tree) {
        this(tree, true);
    }

    CommandTreeVisualizer(@Nullable CommandTree<S> tree, boolean showNodeTypes) {
        this.tree = tree;
        this.showNodeTypes = showNodeTypes;
    }

    public static <S extends CommandSource> CommandTreeVisualizer<S> of(@Nullable CommandTree<S> tree) {
        return new CommandTreeVisualizer<>(tree);
    }

    public void visualize() {
        visualizeSimple();
    }

    public void visualizeSimple() {
        if (tree == null || !ImperatDebugger.isEnabled()) {
            return;
        }
        ImperatDebugger.debug(getVisualizationString());
    }

    public String getVisualizationString() {
        if (tree == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n==== RootCommand Tree ====\n\n");
        renderNode(tree.rootNode(), builder, "", true);
        return builder.toString();
    }

    private void renderNode(Node<S> node, StringBuilder out, String prefix, boolean isTail) {
        out.append(prefix);
        if (!prefix.isEmpty()) {
            out.append(isTail ? "└── " : "├── ");
        }
        out.append(formatNode(node)).append('\n');

        List<Node<S>> children = new ArrayList<>();
        for (Node<S> child : node.getChildren()) {
            children.add(child);
        }

        for (Argument<S> optional : node.getOptionalArguments()) {
            out.append(prefix)
                    .append(prefix.isEmpty() ? "" : (isTail ? "    " : "│   "))
                    .append("   ")
                    .append(showNodeTypes ? "[OPT] " : "")
                    .append(optional.format())
                    .append('\n');
        }

        for (int i = 0; i < children.size(); i++) {
            boolean childTail = i == children.size() - 1;
            String childPrefix = prefix + (prefix.isEmpty() ? "" : (isTail ? "    " : "│   "));
            renderNode(children.get(i), out, childPrefix, childTail);
        }
    }

    private String formatNode(Node<S> node) {
        String base = node.format();
        if (!showNodeTypes) {
            return base;
        }
        if (node.isRoot()) {
            return "[ROOT] " + base;
        }
        if (node.getMainArgument().isCommand()) {
            return "[SUB] " + base;
        }
        if (node.getMainArgument().isOptional()) {
            return "[OPT] " + base;
        }
        if (node.getMainArgument().isFlag()) {
            return "[FLAG] " + base;
        }
        return "[REQ] " + base;
    }
}
