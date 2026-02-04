package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Priority;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.ImperatDebugger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
public final class CommandTreeVisualizer<S extends Source> {
    
    private static final String HORIZONTAL_LINE = "─";
    private static final String VERTICAL_LINE = "│";
    private static final String CORNER_TOP_LEFT = "┌";
    private static final String CORNER_TOP_RIGHT = "┐";
    private static final String CORNER_BOTTOM_LEFT = "└";
    private static final String CORNER_BOTTOM_RIGHT = "┘";
    private static final String T_JUNCTION = "┬";
    private static final String T_JUNCTION_UP = "┴";
    private static final String T_JUNCTION_RIGHT = "├";
    private static final String T_JUNCTION_LEFT = "┤";
    private static final String CROSS_JUNCTION = "┼";
    
    private final @Nullable CommandTree<S> tree;
    private final boolean useColors;
    private final boolean showNodeTypes;
    private final int minNodeWidth;
    private final int nodeSpacing;
    
    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";      // Subcommands
    private static final String YELLOW = "\u001B[33m";    // Required args
    private static final String GREEN = "\u001B[32m";     // Optional flags
    private static final String GRAY = "\u001B[90m";      // Optional args
    private static final String BOLD = "\u001B[1m";
    private static final String WHITE = "\u001B[37m";
    
    CommandTreeVisualizer(@Nullable CommandTree<S> tree) {
        this(tree, true, true, 12, 2);
    }
    
    CommandTreeVisualizer(@Nullable CommandTree<S> tree,
                          boolean useColors,
                          boolean showNodeTypes,
                          int minNodeWidth,
                          int nodeSpacing) {
        this.tree = tree;
        this.useColors = useColors;
        this.showNodeTypes = showNodeTypes;
        this.minNodeWidth = minNodeWidth;
        this.nodeSpacing = nodeSpacing;
    }
    
    public static <S extends Source> CommandTreeVisualizer<S> of(@Nullable CommandTree<S> tree) {
        return new CommandTreeVisualizer<>(tree);
    }
    
    public void visualize() {
        if (tree == null || !ImperatDebugger.isEnabled()) return;
        
        // Build the tree structure
        TreeNode root = buildTreeStructure(tree.rootNode(), null);
        
        // Calculate positions for each node
        calculateNodePositions(root);
        
        // Render the tree to a string
        String visualization = renderTree(root);
        
        ImperatDebugger.debug(visualization);
    }
    
    private TreeNode buildTreeStructure(ParameterNode<S, ?> node, TreeNode parent) {
        if (node == null) return null;
        
        NodeType type = parent == null ? NodeType.ROOT : determineNodeType(node);
        TreeNode treeNode = new TreeNode(node, type);
        
        for (ParameterNode<S, ?> child : node.getChildren()) {
            TreeNode childTreeNode = buildTreeStructure(child, treeNode);
            if (childTreeNode != null) {
                treeNode.children.add(childTreeNode);
            }
        }
        
        return treeNode;
    }
    
    private void calculateNodePositions(TreeNode root) {
        // First pass: calculate widths bottom-up
        calculateWidths(root);
        
        // Second pass: assign positions top-down
        assignPositions(root, 0, 0);
    }
    
    private int calculateWidths(TreeNode node) {
        if (node.children.isEmpty()) {
            node.width = Math.max(minNodeWidth, node.getDisplayText().length() + 4);
            return node.width;
        }
        
        int totalChildrenWidth = 0;
        for (TreeNode child : node.children) {
            totalChildrenWidth += calculateWidths(child);
        }
        
        // Add spacing between children
        totalChildrenWidth += nodeSpacing * (node.children.size() - 1);
        
        // Node width is either its own minimum width or the total width of its children
        node.width = Math.max(
                Math.max(minNodeWidth, node.getDisplayText().length() + 4),
                totalChildrenWidth
        );
        
        return node.width;
    }
    
    private void assignPositions(TreeNode node, int x, int y) {
        node.x = x;
        node.y = y;
        
        if (!node.children.isEmpty()) {
            int currentX = x;
            
            // If node is wider than children total, center the children
            int totalChildrenWidth = 0;
            for (TreeNode child : node.children) {
                totalChildrenWidth += child.width;
            }
            totalChildrenWidth += nodeSpacing * (node.children.size() - 1);
            
            if (node.width > totalChildrenWidth) {
                currentX = x + (node.width - totalChildrenWidth) / 2;
            }
            
            // Assign positions to children
            for (TreeNode child : node.children) {
                assignPositions(child, currentX, y + 4); // 4 lines between levels
                currentX += child.width + nodeSpacing;
            }
        }
    }
    
    private String renderTree(TreeNode root) {
        // Find the dimensions of the canvas
        int maxX = findMaxX(root) + 5;
        int maxY = findMaxY(root) + 3;
        
        // Create a 2D character array as our canvas
        char[][] canvas = new char[maxY][maxX];
        for (char[] row : canvas) {
            Arrays.fill(row, ' ');
        }
        
        // Draw the tree on the canvas
        drawNode(canvas, root);
        
        // Add header and legend
        StringBuilder result = new StringBuilder();
        result.append("\n");
        if (useColors) result.append(BOLD);
        result.append("Command Tree Structure\n");
        if (useColors) result.append(RESET);
        result.append("=".repeat(maxX)).append("\n").append("\n");
        
        // Convert canvas to string
        for (char[] row : canvas) {
            result.append(new String(row).replaceAll("\\s+$", "")).append("\n");
        }
        
        // Add legend
        if (showNodeTypes) {
            result.append("\n").append("Legend:\n");
            if (useColors) {
                result.append("  ").append(CYAN).append("□").append(RESET).append(" Subcommands\n");
                result.append("  ").append(YELLOW).append("□").append(RESET).append(" Required Arguments\n");
                result.append("  ").append(GREEN).append("□").append(RESET).append(" Optional Flags\n");
                result.append("  ").append(GRAY).append("□").append(RESET).append(" Optional Arguments\n");
            } else {
                result.append("  [SUB] Subcommands\n");
                result.append("  [REQ] Required Arguments\n");
                result.append("  [FLAG] Optional Flags\n");
                result.append("  [OPT] Optional Arguments\n");
            }
        }
        
        return result.toString();
    }
    
    private void drawNode(char[][] canvas, TreeNode node) {
        String text = node.getDisplayText();
        int boxWidth = Math.max(text.length() + 2, 10);
        int boxX = node.x + (node.width - boxWidth) / 2;
        int boxY = node.y;
        
        // Draw the box
        drawBox(canvas, boxX, boxY, boxWidth, text);
        
        // Draw connections to children
        if (!node.children.isEmpty()) {
            // Draw vertical line from bottom of parent box
            int parentCenterX = boxX + boxWidth / 2;
            canvas[boxY + 2][parentCenterX] = VERTICAL_LINE.charAt(0);
            
            if (node.children.size() == 1) {
                // Single child - straight line down
                TreeNode child = node.children.get(0);
                int childBoxWidth = Math.max(child.getDisplayText().length() + 2, 10);
                int childCenterX = child.x + (child.width - childBoxWidth) / 2 + childBoxWidth / 2;
                
                // Draw vertical line
                if (parentCenterX == childCenterX) {
                    canvas[boxY + 3][parentCenterX] = VERTICAL_LINE.charAt(0);
                } else {
                    // Need to draw an L-shape
                    canvas[boxY + 3][parentCenterX] = VERTICAL_LINE.charAt(0);
                    
                    int startX = Math.min(parentCenterX, childCenterX);
                    int endX = Math.max(parentCenterX, childCenterX);
                    for (int x = startX; x <= endX; x++) {
                        if (canvas[boxY + 3][x] == ' ') {
                            canvas[boxY + 3][x] = HORIZONTAL_LINE.charAt(0);
                        }
                    }
                    canvas[boxY + 3][parentCenterX] = parentCenterX < childCenterX ? CORNER_BOTTOM_LEFT.charAt(0) : CORNER_BOTTOM_RIGHT.charAt(0);
                    canvas[boxY + 3][childCenterX] = T_JUNCTION.charAt(0);
                }
            } else {
                // Multiple children - draw branching lines
                canvas[boxY + 3][parentCenterX] = T_JUNCTION.charAt(0);
                
                // Find the range of children centers
                int leftmostX = Integer.MAX_VALUE;
                int rightmostX = Integer.MIN_VALUE;
                
                for (TreeNode child : node.children) {
                    int childBoxWidth = Math.max(child.getDisplayText().length() + 2, 10);
                    int childCenterX = child.x + (child.width - childBoxWidth) / 2 + childBoxWidth / 2;
                    leftmostX = Math.min(leftmostX, childCenterX);
                    rightmostX = Math.max(rightmostX, childCenterX);
                }
                
                // Draw horizontal line
                for (int x = leftmostX; x <= rightmostX; x++) {
                    if (canvas[boxY + 3][x] == ' ') {
                        canvas[boxY + 3][x] = HORIZONTAL_LINE.charAt(0);
                    }
                }
                
                // Draw down lines to each child
                for (TreeNode child : node.children) {
                    int childBoxWidth = Math.max(child.getDisplayText().length() + 2, 10);
                    int childCenterX = child.x + (child.width - childBoxWidth) / 2 + childBoxWidth / 2;
                    
                    if (canvas[boxY + 3][childCenterX] == HORIZONTAL_LINE.charAt(0)) {
                        canvas[boxY + 3][childCenterX] = T_JUNCTION.charAt(0);
                    }
                    /*else if (canvas[boxY + 3][childCenterX] == T_JUNCTION.charAt(0)) {
                        // Already set
                    }*/
                }
            }
        }
        
        // Recursively draw children
        for (TreeNode child : node.children) {
            drawNode(canvas, child);
        }
    }
    
    private void drawBox(char[][] canvas, int x, int y, int width, String text) {
        // Ensure we don't go out of bounds
        if (y >= canvas.length || x + width >= canvas[0].length) return;
        
        // Top border
        canvas[y][x] = CORNER_TOP_LEFT.charAt(0);
        for (int i = 1; i < width - 1; i++) {
            canvas[y][x + i] = HORIZONTAL_LINE.charAt(0);
        }
        canvas[y][x + width - 1] = CORNER_TOP_RIGHT.charAt(0);
        
        // Middle with text
        canvas[y + 1][x] = VERTICAL_LINE.charAt(0);
        
        // Center the text
        int textStart = x + 1 + (width - 2 - text.length()) / 2;
        for (int i = 0; i < text.length() && textStart + i < x + width - 1; i++) {
            canvas[y + 1][textStart + i] = text.charAt(i);
        }
        
        canvas[y + 1][x + width - 1] = VERTICAL_LINE.charAt(0);
        
        // Bottom border
        canvas[y + 2][x] = CORNER_BOTTOM_LEFT.charAt(0);
        for (int i = 1; i < width - 1; i++) {
            canvas[y + 2][x + i] = HORIZONTAL_LINE.charAt(0);
        }
        canvas[y + 2][x + width - 1] = CORNER_BOTTOM_RIGHT.charAt(0);
        
        // Add color markers if needed (these would need special handling in actual output)
        if (useColors && showNodeTypes) {
            // This is a simplified approach - in reality I'd need to handle ANSI codes differently
            // as they don't fit well in a char array
        }
    }
    
    private int findMaxX(TreeNode node) {
        int max = node.x + node.width;
        for (TreeNode child : node.children) {
            max = Math.max(max, findMaxX(child));
        }
        return max;
    }
    
    private int findMaxY(TreeNode node) {
        int max = node.y + 3; // Box height is 3
        for (TreeNode child : node.children) {
            max = Math.max(max, findMaxY(child));
        }
        return max;
    }
    
    private NodeType determineNodeType(ParameterNode<S, ?> node) {
        // Adjust based on your actual node structure
        String format = node.format().toLowerCase();
        if (format.startsWith("-") || format.startsWith("--")) {
            return NodeType.OPTIONAL_FLAG;
        } else if (format.startsWith("<") && format.endsWith(">")) {
            return NodeType.REQUIRED_ARG;
        } else if (format.startsWith("[") && format.endsWith("]")) {
            return NodeType.OPTIONAL_ARG;
        } else {
            return NodeType.SUBCOMMAND;
        }
    }
    
    private class TreeNode {
        ParameterNode<S, ?> node;
        NodeType type;
        List<TreeNode> children = new ArrayList<>();
        int x, y;  // Position in the canvas
        int width; // Width needed for this subtree
        
        TreeNode(ParameterNode<S, ?> node, NodeType type) {
            this.node = node;
            this.type = type;
        }
        
        String getDisplayText() {
            String base = node.format();
            if (showNodeTypes && type != NodeType.ROOT) {
                switch (type) {
                    case SUBCOMMAND:
                        return "[SUB] " + base;
                    case REQUIRED_ARG:
                        return "[REQ] " + base;
                    case OPTIONAL_FLAG:
                        return "[FLAG] " + base;
                    case OPTIONAL_ARG:
                        return "[OPT] " + base;
                }
            }
            return base;
        }
    }
    
    private enum NodeType {
        ROOT,
        SUBCOMMAND,
        REQUIRED_ARG,
        OPTIONAL_FLAG,
        OPTIONAL_ARG
    }
    
    /**
     * Alternative visualization using a simpler node representation
     */
    public void visualizeSimple() {
        if (tree == null || !ImperatDebugger.isEnabled()) return;
        
        StringBuilder builder = new StringBuilder();
        builder.append("\n==== Command Tree ====\n\n");
        
        visualizeSimpleNode(tree.rootNode(), builder, 0, new ArrayList<>(), true);
        
        ImperatDebugger.debug(builder.toString());
    }
    
    
    /**
     * Alternative visualization using a simpler node representation
     */
    public void visualizeUniqueTreeSimple() {
        if (tree == null || !ImperatDebugger.isEnabled()) return;
        
        StringBuilder builder = new StringBuilder();
        builder.append("\n==== Command Tree ====\n\n");
        
        visualizeSimpleNode(tree.uniqueVersionedTree(), builder, 0, new ArrayList<>(), true);
        
        ImperatDebugger.debug(builder.toString());
    }
    
    private void visualizeSimpleNode(ParameterNode<S, ?> node,
                                     StringBuilder builder,
                                     int depth,
                                     List<Boolean> lastFlags,
                                     boolean isLast) {
        // Draw connection lines
        for (int i = 0; i < depth - 1; i++) {
            builder.append(lastFlags.get(i) ? "     " : "  " + VERTICAL_LINE + "  ");
        }
        
        if (depth > 0) {
            builder.append(isLast ? "  " + CORNER_BOTTOM_LEFT + HORIZONTAL_LINE + " " : "  " + T_JUNCTION_RIGHT + HORIZONTAL_LINE + " ");
        }
        
        // Draw node box
        String nodeText =
                node.format() + ": " + (node.isExecutable() ? "Executable" : "Non-executable") + ":P=" + (node.priority() == Priority.MAXIMUM ?
                                                                                                                  "MAX" :
                                                                                                                  node.priority().getLevel());
        builder.append(nodeText).append("\n");
        
        // Draw children
        List<ParameterNode<S, ?>> children = node.getChildren();
        List<Boolean> newLastFlags = new ArrayList<>(lastFlags);
        if (depth > 0) {
            newLastFlags.add(isLast);
        }

        int i = 0;
        for (var child : children) {
            visualizeSimpleNode(child, builder, depth + 1, newLastFlags, i == children.size() - 1);
            i++;
        }

    }
}