package studio.mevera.imperat.command.tree.help.renderers.tree;

import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.HelpTheme;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.util.List;

public class HierarchicalTreeRenderer<S extends Source> implements HelpLayoutRenderer<S, String> {
    
    @Override
    public void render(
            ExecutionContext<S> context,
            String data,
            HelpEntryList<S> helpEntries,
            HelpRenderOptions options
    ) {
        HelpTheme theme = options.getTheme();
        S source = context.source();
        
        // Instead of using entries, get the root node directly!
        CommandTree<S> tree = context.command().tree();
        ParameterNode<S, ?> root = tree.rootNode();
        
        // Header
        source.reply(theme.formatHeader(theme.getHeaderMessage()));
        
        // Render the actual tree structure
        renderNode(source, root, helpEntries, options, "", true);
    }
    
    private void renderNode(
            S source,
            ParameterNode<S, ?> node,
            HelpEntryList<S> entries,
            HelpRenderOptions options,
            String prefix,
            boolean isLast
    ) {
        HelpTheme theme = options.getTheme();
        
        if (node.getDepth() >= 0) { // Skip root node itself
            StringBuilder line = new StringBuilder(prefix);
            
            // Add branch
            if (!prefix.isEmpty()) {
                line.append(theme.getTreeBranch(isLast));
            }
            
            // Format node
            line.append(theme.formatCommand(node.format()));
            
            // Mark if executable
            if (node.isExecutable()) {
                line.append(" ").append(theme.formatDescription("[executable]"));
            }
            
            source.reply(line.toString());
        }
        
        // Render children
        List<ParameterNode<S, ?>> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            ParameterNode<S, ?> child = children.get(i);
            boolean childIsLast = (i == children.size() - 1);
            
            String childPrefix = prefix;
            if (node.getDepth() >= 0) {
                childPrefix += theme.getTreeIndent(!isLast);
            }
            
            renderNode(source, child, entries, options, childPrefix, childIsLast);
        }
    }
    
}