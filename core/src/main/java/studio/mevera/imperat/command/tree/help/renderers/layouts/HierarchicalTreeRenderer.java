package studio.mevera.imperat.command.tree.help.renderers.layouts;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.HelpTheme;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.util.LinkedList;

final class HierarchicalTreeRenderer<S extends Source> implements HelpLayoutRenderer<S, String> {
    
    @Override
    public void render(
            @NotNull ExecutionContext<S> context,
            @NotNull String data,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpRenderOptions<S> options
    ) {
        HelpTheme<S> theme = options.getTheme();
        S source = context.source();
        
        // Header
        if (theme.isShowHeader()) {
            source.reply(theme.formatHeader(context));
        }
        
        // Check if empty
        if (helpEntries.isEmpty()) {
            source.reply(theme.formatEmpty(context));
            if (theme.isShowFooter()) {
                source.reply(theme.formatFooter(context));
            }
            return;
        }
        
        CommandTree<S> tree = context.command().tree();
        ParameterNode<S, ?> root = tree.rootNode();
        
        // Render tree
        renderNode(source, root, helpEntries, options, "", true, 0);
        
        if (theme.isShowFooter()) {
            source.reply(theme.formatFooter(context));
        }
    }
    
    private void renderNode(
            S source,
            ParameterNode<S, ?> node,
            HelpEntryList<S> entries,
            HelpRenderOptions<S> options,
            String prefix,
            boolean isLast,
            int currentDepth
    ) {
        HelpTheme<S> theme = options.getTheme();
        
        // Check max depth
        /*if (theme.getMaxDepth() > 0 && currentDepth > theme.getMaxDepth()) {
            return;
        }
        
        // Check max entries
        if (theme.getMaxEntries() > 0 && renderCount[0] >= theme.getMaxEntries()) {
            return;
        }
        */
        
        
        // Render current node
        if (node.getParent() != null) {
            StringBuilder line = new StringBuilder();
            
            // Add root command if needed
            if (node.getDepth() == 0 && theme.isShowRootCommand()) {
                line.append(theme.getCommandPrefix())
                        .append(theme.formatCommand(node.getParent().format()))
                        .append(theme.getPathSeparator());
            }
            
            // Add prefix and branch
            line.append(prefix);
            if (!prefix.isEmpty()) {
                line.append(theme.getTreeBranch(isLast));
            }
            
            // Format node based on type
            if (node.isCommand()) {
                line.append(theme.formatSubcommand(node.format()));
            } else if (node.isOptional()) {
                line.append(theme.formatArgument(node.getData().name(), true));
            } else if (node.isFlag()) {
                line.append(theme.formatFlag(node.getData().name()));
            } else {
                line.append(theme.formatArgument(node.getData().name(), false));
            }
            
            // Add description if executable and enabled
            if (node.isExecutable() && theme.isShowDescriptions()) {
                CommandUsage<S> usage = node.getExecutableUsage();
                if (usage != null && usage.description() != null) {
                    line.append(theme.getDescriptionSeparator())
                            .append(theme.formatDescription(usage.description().toString()));
                }
            }
            
            // Add permission if enabled
            if (theme.isShowPermissions() && node.getPermission() != null) {
                line.append(" ").append(theme.formatPermission(node.getPermission()));
            }
            
            source.reply(line.toString());
        }
        
        // Render children (unless in compact mode for non-executable nodes)
        if (!theme.isCompactMode() || node.isExecutable()) {
            LinkedList<ParameterNode<S, ?>> children = new LinkedList<>(node.getChildren());
            while (!children.isEmpty()) {
                ParameterNode<S, ?> child = children.pop();
                boolean childIsLast = children.isEmpty();
                
                String childPrefix = prefix;
                if (node.getDepth() >= 0) {
                    childPrefix += theme.getTreeIndent(!isLast);
                }
                
                renderNode(source, child, entries, options, childPrefix, childIsLast,
                        currentDepth + 1);
            }
        }
    }
}