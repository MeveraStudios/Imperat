package studio.mevera.imperat.command.tree.help.renderers.layouts;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import java.util.LinkedList;

final class TreeHelpRenderer<S extends Source, C> implements HelpLayoutRenderer<S, C> {
    
    @Override
    public void render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpRenderOptions<S, C> options
    ) {
        S source = context.source();
        
        CommandTree<S> tree = context.command().tree();
        ParameterNode<S, ?> root = tree.rootNode();
        
        // Render tree
        renderNode(source, context, root, helpEntries, options, "", true, 0);
        
    }
    
    private void renderNode(
            S source,
            ExecutionContext<S> context,
            ParameterNode<S, ?> node,
            HelpEntryList<S> entries,
            HelpRenderOptions<S, C> options,
            String prefix,
            boolean isLast,
            int currentDepth
    ) {
        var theme = options.getTheme();
        
        // Render current node
        if (node.getParent() != null) {
            StringBuilder line = new StringBuilder();
            
            // Add root command if needed
            if (node.getDepth() == 0 ) {
                line.append("/")
                        .append(node.getParent().format())
                        .append(theme.getPathSeparator());
            }
            
            // Add prefix and branch
            line.append(prefix);
            if (!prefix.isEmpty()) {
                line.append(theme.getTreeBranch(isLast));
            }
            
            // Format node based on type
            line.append(node.format());
            
            
            source.reply(line.toString());
        }
        
        // Render children (unless in compact mode for non-executable nodes)
        if (node.isExecutable()) {
            LinkedList<ParameterNode<S, ?>> children = new LinkedList<>(node.getChildren());
            while (!children.isEmpty()) {
                ParameterNode<S, ?> child = children.pop();
                boolean childIsLast = children.isEmpty();
                
                String childPrefix = prefix;
                if (node.getDepth() >= 0) {
                    childPrefix += theme.getTreeIndent(!isLast);
                }
                
                renderNode(source, context, child, entries, options, childPrefix, childIsLast,
                        currentDepth + 1);
            }
        }
    }
}