package studio.mevera.imperat.command.tree.help.renderers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.theme.HelpComponent;
import studio.mevera.imperat.command.tree.help.theme.HelpTheme;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Standard help renderer that supports both tree and flat presentation styles.
 * Uses theme-driven layout selection and properly processes the HelpEntryList
 * with efficient direct path analysis using parameter.position() and parameter.isCommand().
 *
 * @param <S> the source type
 * @param <C> the component type
 */
public final class StandardHelpRenderer<S extends Source, C> implements HelpLayoutRenderer<S, C> {
    
    @Override
    public void render(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpTheme<S, C> theme
    ) {
        if (theme.getPreferredStyle() == HelpTheme.PresentationStyle.TREE) {
            renderTree(context, helpEntries, theme);
        } else {
            renderFlat(context, helpEntries, theme);
        }
    }
    
    /**
     * Renders help entries in a flat list format.
     */
    private void renderFlat(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpTheme<S, C> theme
    ) {
        S source = context.source();
        
        for (HelpEntry<S> entry : helpEntries) {
            HelpComponent<S, C> component = theme.getUsageFormatter().format(
                    context.command(),
                    entry.getPathway(),
                    context,
                    theme
            );
            
            component.send(source);
        }
    }
    
    /**
     * Renders help entries in a tree structure format.
     * Uses parameter.position() and parameter.isCommand() to build hierarchy efficiently.
     */
    private void renderTree(
            @NotNull ExecutionContext<S> context,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpTheme<S, C> theme
    ) {
        if (helpEntries.isEmpty()) return;
        
        // Sort entries by their subcommand paths
        List<HelpEntry<S>> sortedEntries = sortBySubcommandPath(helpEntries);
        
        // Track rendered subcommand paths to avoid duplicates
        Set<String> renderedPaths = new HashSet<>();
        
        // Render each entry's subcommand hierarchy
        for (HelpEntry<S> entry : sortedEntries) {
            renderSubcommandHierarchy(context, entry, sortedEntries, theme, renderedPaths);
        }
    }
    
    /**
     * Renders the subcommand hierarchy for a single entry using parameter positions.
     */
    private void renderSubcommandHierarchy(
            ExecutionContext<S> context,
            HelpEntry<S> entry,
            List<HelpEntry<S>> allEntries,
            HelpTheme<S, C> theme,
            Set<String> renderedPaths
    ) {
        CommandUsage<S> usage = entry.getPathway();
        S source = context.source();
        
        // Get all subcommand positions from this usage
        List<Integer> subcommandPositions = getSubcommandPositions(usage);
        
        // Render each subcommand level
        for (int pos : subcommandPositions) {
            var parameter = usage.getParameter(pos);
            
            String pathUpToPos = buildSubcommandPath(usage, pos);
            
            if (!renderedPaths.contains(pathUpToPos)) {
                renderedPaths.add(pathUpToPos);
                
                boolean isLastSubcommand = (pos == subcommandPositions.get(subcommandPositions.size() - 1));
                boolean hasChildSubcommands = hasChildSubcommands(allEntries, usage, pos);
                
                // Build tree prefix using position
                HelpComponent<S, C> prefix = buildPrefixForPosition(allEntries, entry, pos, theme);
                
                // Add branch symbol
                boolean isLastSibling = isLastSiblingAtPosition(allEntries, entry, pos);
                if (pos > 0) {
                    HelpComponent<S, C> branch = theme.getTreeBranch(isLastSibling);
                    prefix = prefix.append(branch);
                }
                
                HelpComponent<S, C> line = prefix;
                
                if (isLastSubcommand && !hasChildSubcommands) {
                    // Final subcommand - show complete formatted usage (including arguments)
                    HelpComponent<S, C> formatted = theme.getUsageFormatter().format(
                            context.command(),
                            usage,
                            context,
                            theme
                    );
                    line = line.append(formatted);
                } else {
                    // Intermediate subcommand - show just the subcommand name
                    assert parameter != null;
                    line = line.appendParameterFormat(parameter);
                }
                
                line.send(source);
            }
        }
    }
    
    /**
     * Gets all subcommand positions from a usage, sorted by position.
     */
    private List<Integer> getSubcommandPositions(CommandUsage<S> usage) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < usage.size(); i++) {
            var parameter = usage.getParameter(i);
            assert parameter != null;
            if (parameter.isCommand()) {
                positions.add(parameter.position());
            }
        }
        Collections.sort(positions);
        return positions;
    }
    
    /**
     * Builds subcommand path up to the specified position.
     */
    private String buildSubcommandPath(CommandUsage<S> usage, int upToPosition) {
        StringBuilder path = new StringBuilder();
        boolean first = true;
        
        for (int i = 0; i < usage.size(); i++) {
            var parameter = usage.getParameter(i);
            assert parameter != null;
            if (parameter.isCommand() && parameter.position() <= upToPosition) {
                if (!first) path.append("/");
                path.append(parameter.name());
                first = false;
            }
        }
        return path.toString();
    }
    
    /**
     * Checks if a subcommand at the given position has child subcommands.
     */
    private boolean hasChildSubcommands(List<HelpEntry<S>> allEntries, CommandUsage<S> currentUsage, int position) {
        String currentPath = buildSubcommandPath(currentUsage, position);
        
        for (HelpEntry<S> otherEntry : allEntries) {
            CommandUsage<S> otherUsage = otherEntry.getPathway();
            
            // Look for entries that extend our path with more subcommands
            for (int i = 0; i < otherUsage.size(); i++) {
                var parameter = otherUsage.getParameter(i);
                assert parameter != null;
                if (parameter.isCommand() && parameter.position() > position) {
                    String otherPath = buildSubcommandPath(otherUsage, position);
                    if (currentPath.equals(otherPath)) {
                        return true; // Found a child subcommand
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Determines if this entry is the last sibling at the given position.
     */
    private boolean isLastSiblingAtPosition(List<HelpEntry<S>> allEntries, HelpEntry<S> currentEntry, int position) {
        CommandUsage<S> currentUsage = currentEntry.getPathway();
        var currentParameter = currentUsage.getParameter(position);
        
        if (position == 0) {
            // Check if this is the last root-level subcommand
            assert currentParameter != null;
            String currentSubcommand = currentParameter.name();
            
            for (HelpEntry<S> otherEntry : allEntries) {
                CommandUsage<S> otherUsage = otherEntry.getPathway();
                for (int i = 0; i < otherUsage.size(); i++) {
                    var otherParameter = otherUsage.getParameter(i);
                    assert otherParameter != null;
                    if (otherParameter.isCommand() && otherParameter.position() == 0) {
                        String otherSubcommand = otherParameter.name();
                        if (otherSubcommand.compareTo(currentSubcommand) > 0) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } else {
            // Check siblings at this position with same parent path
            String parentPath = buildSubcommandPath(currentUsage, position - 1);
            assert currentParameter != null;
            String currentSubcommand = currentParameter.name();
            
            for (HelpEntry<S> otherEntry : allEntries) {
                CommandUsage<S> otherUsage = otherEntry.getPathway();
                for (int i = 0; i < otherUsage.size(); i++) {
                    var otherParameter = otherUsage.getParameter(i);
                    assert otherParameter != null;
                    if (otherParameter.isCommand() && otherParameter.position() == position) {
                        String otherParentPath = buildSubcommandPath(otherUsage, position - 1);
                        if (parentPath.equals(otherParentPath)) {
                            String otherSubcommand = otherParameter.name();
                            if (otherSubcommand.compareTo(currentSubcommand) > 0) {
                                return false; // Found a later sibling
                            }
                        }
                    }
                }
            }
            return true;
        }
    }
    
    /**
     * Builds the tree prefix (indentation) for a given position.
     */
    private HelpComponent<S, C> buildPrefixForPosition(
            List<HelpEntry<S>> allEntries,
            HelpEntry<S> currentEntry,
            int targetPosition,
            HelpTheme<S, C> theme
    ) {
        if (targetPosition <= 0) {
            return theme.createEmptyComponent();
        }
        
        HelpComponent<S, C> prefix = theme.createEmptyComponent();
        
        // Build indentation for each parent position
        for (int pos = 0; pos < targetPosition; pos++) {
            boolean hasMoreSiblings = !isLastSiblingAtPosition(allEntries, currentEntry, pos);
            HelpComponent<S, C> indent = theme.getTreeIndent(hasMoreSiblings);
            prefix = prefix.append(indent);
        }
        
        return prefix;
    }
    
    /**
     * Sorts help entries by their subcommand paths for proper hierarchy rendering.
     */
    private List<HelpEntry<S>> sortBySubcommandPath(HelpEntryList<S> entries) {
        List<HelpEntry<S>> sorted = new ArrayList<>();
        for (HelpEntry<S> entry : entries) {
            sorted.add(entry);
        }
        
        sorted.sort((a, b) -> {
            CommandUsage<S> pathA = a.getPathway();
            CommandUsage<S> pathB = b.getPathway();
            
            // Get subcommand positions for comparison
            List<Integer> positionsA = getSubcommandPositions(pathA);
            List<Integer> positionsB = getSubcommandPositions(pathB);
            
            // Compare subcommands by their positions
            int minPositions = Math.min(positionsA.size(), positionsB.size());
            for (int i = 0; i < minPositions; i++) {
                int posA = positionsA.get(i);
                int posB = positionsB.get(i);
                
                String commandA = Objects.requireNonNull(pathA.getParameter(posA)).name();
                String commandB = Objects.requireNonNull(pathB.getParameter(posB)).name();
                int cmp = commandA.compareTo(commandB);
                if (cmp != 0) {
                    return cmp;
                }
            }
            
            // Shorter subcommand paths come first
            return Integer.compare(positionsA.size(), positionsB.size());
        });
        
        return sorted;
    }
    
}