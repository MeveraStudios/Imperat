package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.ImperatDebugger;

@ApiStatus.Internal
public final class CommandTreeVisualizer<S extends Source> {

    private final @Nullable CommandTree<S> tree;

    CommandTreeVisualizer(@Nullable CommandTree<S> tree) {
        this.tree = tree;
    }

    public static <S extends Source> CommandTreeVisualizer<S> of(@Nullable CommandTree<S> tree) {
        return new CommandTreeVisualizer<>(tree);
    }


    public void visualize() {
        if (tree == null || !ImperatDebugger.isEnabled()) return;
        StringBuilder builder = new StringBuilder();
        visualizeNode(tree.rootNode(), builder, 0);
        ImperatDebugger.debug(builder.toString());
    }

    private void visualizeNode(ParameterNode<S, ?> node, StringBuilder builder, int depth) {
        if (node == null) {
            return;
        }
        builder.append("  ".repeat(Math.max(0, depth)));
        builder.append(node.format()).append("\n");
        for (ParameterNode<S, ?> child : node.getChildren()) {
            visualizeNode(child, builder, depth + 1);
        }

    }

}
