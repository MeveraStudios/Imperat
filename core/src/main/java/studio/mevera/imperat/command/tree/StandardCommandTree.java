package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.tree.help.HelpEntryFactory;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpFilter;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.resolvers.PermissionChecker;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;
import java.util.*;

/**
 * N-ary tree implementation focused on maximum performance
 * @author Mqzen
 */
final class StandardCommandTree<S extends Source> implements CommandTree<S> {
    private final Command<S> rootCommand;
    final CommandNode<S> root;
    final CommandNode<S> uniqueRoot;
    
    private int size, uniqueSize;
    
    // Pre-computed immutable collections to eliminate allocations
    private final static int INITIAL_SUGGESTIONS_CAPACITY = 20;
    
    // Optimized flag cache with better hashing
    
    // Pre-sized collections for common operations
    private final ThreadLocal<ArrayList<ParameterNode<S, ?>>> pathBuffer =
            ThreadLocal.withInitial(() -> new ArrayList<>(16));
    
    private final ThreadLocal<ArrayList<CommandParameter<S>>> paramBuffer =
            ThreadLocal.withInitial(() -> new ArrayList<>(8));
    
    private final ImperatConfig<S> imperatConfig;
    private final @NotNull PermissionChecker<S> permissionChecker;
    
    private final HelpEntryFactory<S> helpEntryFactory = HelpEntryFactory.defaultFactory();
    
    private final Map<CommandParameter<S>, String> assignedPermissions = new HashMap<>();
    
    
    StandardCommandTree(ImperatConfig<S> imperatConfig, Command<S> command) {
        this.rootCommand = command;
        this.root = ParameterNode.createCommandNode(null, command, -1, command.getDefaultUsage());
        this.imperatConfig = imperatConfig;
        this.permissionChecker = imperatConfig.getPermissionChecker();
        this.uniqueRoot = root.copy();
    }
    
    // Optimized parsing with reduced allocations
    public void parseCommandUsages() {
        final var usages = root.data.usages();
        for (var usage : usages) {
            parseUsage(usage);
        }
        //computePermissions();
    }
    
    @Override
    public @NotNull Command<S> root() {
        return rootCommand;
    }
    
    @Override
    public @NotNull CommandNode<S> rootNode() {
        return root;
    }
    
    @Override
    public @NotNull CommandNode<S> uniqueVersionedTree() {
        return uniqueRoot;
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @Override
    public int uniqueSize() {
        return uniqueSize;
    }
    
    @Override
    public void parseUsage(@NotNull CommandUsage<S> usage) {
        // Register flags once
        final var flags = usage.getUsedFreeFlags();
        for (var flag : flags) {
            rootCommand.registerFlag(flag);
        }
        
        final var parameters = usage.getParameters();
        if (usage.isDefault()) {
            root.setExecutableUsage(usage);
            uniqueRoot.setExecutableUsage(usage);
        }
        
        
        // Use thread-local buffer to eliminate allocations
        final var path = pathBuffer.get();
        path.clear();
        path.add(root);
        
        try {
            addParametersToTree(root, usage, parameters, 0, path);
        } finally {
            path.clear(); // Clean up for next use
        }
        
        //adding unique versioned tree
        addParametersWithoutOptionalBranchingToTree(uniqueRoot, usage, parameters, 0);
    }
    
    @Override
    public void computePermissions() {
        root.setPermission(rootCommand.getSinglePermission());
        uniqueRoot.setPermission(rootCommand.getSinglePermission());
        
        if(!imperatConfig.isAutoPermissionAssignMode()) {
            return;
        }
        var rootPerm = imperatConfig.getPermissionLoader().load(rootCommand);
        root.setPermission(rootPerm);
        
        for(var child : root.getChildren()) {
            computePermissionsRecursive(child, new ArrayList<>(), rootPerm);
        }
    }
    
    @Override
    public @Nullable String getAutoAssignedPermission(@NotNull CommandParameter<S> commandParameter) {
        if(!imperatConfig.isAutoPermissionAssignMode()) {
            throw new IllegalStateException("APA mode must be enabled!");
        }
        
        return assignedPermissions.get(commandParameter);
    }
    
    private void computePermissionsRecursive(ParameterNode<S, ?> node, List<ParameterNode<S, ?>> pathNodes, String rootPermission) {
        // Add current node to the path
        List<ParameterNode<S, ?>> currentPath = new ArrayList<>(pathNodes);
        currentPath.add(node);
        
        // If this node is executable, assign it a permission
        if(node.getPermission() == null) {
            if (node.isExecutable() || node.isCommand()) {
                String permission = buildHierarchicalPermission(rootPermission, currentPath);
                imperatConfig.getPermissionAssigner().assign(node, permission);
                assignedPermissions.put(node.data, permission);
            } else {
                ParameterNode<S, ?> firstParentCmd = node;
                while (firstParentCmd != null) {
                    if (firstParentCmd.isCommand()) {
                        break;
                    }
                    firstParentCmd = firstParentCmd.getParent();
                }
                if (firstParentCmd == null) {
                    firstParentCmd = root;
                }
                imperatConfig.getPermissionAssigner().assign(node, firstParentCmd.getPermission());
                assignedPermissions.put(node.data, firstParentCmd.getPermission());
            }
        }
        
        // Continue recursion for children
        if(!node.getChildren().isEmpty()) {
            for(var child : node.getChildren()) {
                computePermissionsRecursive(child, currentPath, rootPermission);
            }
        }
    }
    
    private String buildHierarchicalPermission(String root, List<ParameterNode<S, ?>> pathNodes) {
        // Find the base permission level (required parameters only)
        StringBuilder basePermission = new StringBuilder(root);
        
        // Add only required parameters to build the base permission
        for(ParameterNode<S, ?> node : pathNodes) {
            if(!node.isOptional()) {
                String component = imperatConfig.getPermissionLoader().load(node.data);
                basePermission.append(imperatConfig.getPermissionAssigner().getPermissionDelimiter())
                        .append(component);
            }
        }
        
        // For the current node, if it's optional, add it at the same depth level
        ParameterNode<S, ?> currentNode = pathNodes.get(pathNodes.size() - 1);
        if(currentNode.isOptional()) {
            String component = imperatConfig.getPermissionLoader().load(currentNode.data);
            return basePermission + imperatConfig.getPermissionAssigner()
                    .getPermissionDelimiter() + component;
        }
        
        // If current node is required, it's already included in base permission
        return basePermission.toString();
    }
    
    private void addParametersToTree(
            ParameterNode<S, ?> currentNode,
            CommandUsage<S> usage,
            List<CommandParameter<S>> parameters,
            int index,
            List<ParameterNode<S, ?>> path
    ) {
        final int paramSize = parameters.size();
        if (index >= paramSize) {
            currentNode.setExecutableUsage(usage);
            return;
        }
        
        
        if (currentNode.isGreedyParam()) {
            if (!currentNode.isLast()) {
                throw new IllegalStateException("A greedy node '%s' is not the last argument!".formatted(currentNode.format()));
            }
            currentNode.setExecutableUsage(usage);
            return;
        }

        // Optimized flag sequence detection
        int flagSequenceEnd = findFlagSequenceEnd(parameters, index);
        
        if (flagSequenceEnd > index) {
            // Handle multiple consecutive optional flags
            handleFlagSequenceOptimized(currentNode, usage, parameters, index, flagSequenceEnd, path);
            addParametersToTree(currentNode, usage, parameters, flagSequenceEnd, path);
            return;
        }
        
        // Regular parameter handling
        final var param = parameters.get(index);
        final var childNode = getOrCreateChildNode(currentNode, param, true);
        
        // Efficient path management
        final int pathSize = path.size();
        path.add(childNode);
        
        try {
            addParametersToTree(childNode, usage, parameters, index + 1, path);
            
            if (param.isOptional()) {
                // Create sublist view instead of new list
                addParametersToTree(currentNode, usage, parameters, index + 1,
                        path.subList(0, pathSize));
            }
        } finally {
            // Restore path size efficiently
            if (path.size() > pathSize) {
                path.remove(pathSize);
            }
        }
    }
    
    private void addParametersWithoutOptionalBranchingToTree(
            ParameterNode<S, ?> currentNode,
            CommandUsage<S> usage,
            List<CommandParameter<S>> parameters,
            int index
    ) {
        final int paramSize = parameters.size();
        if (index >= paramSize) {
            currentNode.setExecutableUsage(usage);
            return;
        }
        
        if (currentNode.isGreedyParam()) {
            if (!currentNode.isLast()) {
                throw new IllegalStateException("A greedy node '%s' is not the last argument!".formatted(currentNode.format()));
            }
            currentNode.setExecutableUsage(usage);
            return;
        }
        
        // Regular parameter handling
        final var param = parameters.get(index);
        final var childNode = getOrCreateChildNode(currentNode, param, true);
        
        addParametersWithoutOptionalBranchingToTree(
                childNode, usage, parameters, index + 1
        );
    }
    
    /**
     * Optimized flag sequence detection in single pass
     */
    private int findFlagSequenceEnd(List<CommandParameter<S>> parameters, int startIndex) {
        if (startIndex >= parameters.size()) return startIndex;
        
        final var startParam = parameters.get(startIndex);
        if (!startParam.isFlag() || !startParam.isOptional()) {
            return startIndex;
        }
        
        int end = startIndex + 1;
        for (int i = startIndex + 1; i < parameters.size(); i++) {
            final var param = parameters.get(i);
            if (param.isFlag() && param.isOptional()) {
                end = i + 1;
            } else {
                break;
            }
        }
        
        return end;
    }
    
    
    /**
     * Optimized flag permutation handling - FIXED VERSION
     * Now generates all possible combinations (subsets) of flags, not just full permutations
     */
    private void handleFlagSequenceOptimized(
            ParameterNode<S, ?> currentNode,
            CommandUsage<S> usage,
            List<CommandParameter<S>> allParameters,
            int flagStart,
            int flagEnd,
            List<ParameterNode<S, ?>> path
    ) {
        final var flagParams = paramBuffer.get();
        flagParams.clear();
        
        try {
            // Collect flag parameters
            for (int i = flagStart; i < flagEnd; i++) {
                flagParams.add(allParameters.get(i));
            }
            
            // Generate all possible combinations (subsets) of flags
            generateAllFlagCombinations(currentNode, usage, allParameters, flagParams, flagEnd, path);
        } finally {
            flagParams.clear();
        }
    }
    
    /**
     * NEW METHOD: Generates all possible combinations (subsets) of optional flags
     */
    private void generateAllFlagCombinations(
            ParameterNode<S, ?> currentNode,
            CommandUsage<S> usage,
            List<CommandParameter<S>> allParameters,
            List<CommandParameter<S>> flagParams,
            int nextIndex,
            List<ParameterNode<S, ?>> basePath
    ) {
        final int flagCount = flagParams.size();
        
        // Generate all possible subsets using bit manipulation
        // For n flags, we have 2^n possible combinations (including empty set)
        final int totalCombinations = 1 << flagCount; // 2^n
        
        for (int mask = 0; mask < totalCombinations; mask++) {
            // Create subset based on bitmask
            final var subset = new ArrayList<CommandParameter<S>>();
            for (int i = 0; i < flagCount; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(flagParams.get(i));
                }
            }
            
            if (subset.isEmpty()) {
                // Empty subset - just continue with remaining parameters
                if (nextIndex < allParameters.size()) {
                    addParametersToTree(currentNode, usage, allParameters, nextIndex, basePath);
                } else {
                    currentNode.setExecutableUsage(usage);
                }
            } else {
                // Non-empty subset - generate all permutations of this subset
                generatePermutationsForSubset(currentNode, usage, allParameters, subset, nextIndex, basePath);
            }
        }
    }
    
    /**
     * NEW METHOD: Generates all permutations for a specific subset of flags
     */
    private void generatePermutationsForSubset(
            ParameterNode<S, ?> currentNode,
            CommandUsage<S> usage,
            List<CommandParameter<S>> allParameters,
            List<CommandParameter<S>> subset,
            int nextIndex,
            List<ParameterNode<S, ?>> basePath
    ) {
        if (subset.size() <= 3) {
            generateSmallPermutationsForSubset(currentNode, usage, allParameters, subset, nextIndex, basePath);
        } else {
            generateLargePermutationsForSubset(currentNode, usage, allParameters, subset, nextIndex, basePath);
        }
    }
    
    /**
     * MODIFIED METHOD: Handle small permutations for subsets
     */
    private void generateSmallPermutationsForSubset(
            ParameterNode<S, ?> currentNode,
            CommandUsage<S> usage,
            List<CommandParameter<S>> allParameters,
            List<CommandParameter<S>> subset,
            int nextIndex,
            List<ParameterNode<S, ?>> basePath
    ) {
        final int size = subset.size();
        if (size == 1) {
            processPermutationPath(currentNode, usage, allParameters, subset, nextIndex, basePath);
        } else if (size == 2) {
            final var flag1 = subset.get(0);
            final var flag2 = subset.get(1);
            
            processPermutationPath(currentNode, usage, allParameters, List.of(flag1, flag2), nextIndex, basePath);
            processPermutationPath(currentNode, usage, allParameters, List.of(flag2, flag1), nextIndex, basePath);
        } else if (size == 3) {
            final var flag1 = subset.get(0);
            final var flag2 = subset.get(1);
            final var flag3 = subset.get(2);
            
            // All 6 permutations of 3 flags
            processPermutationPath(currentNode, usage, allParameters, List.of(flag1, flag2, flag3), nextIndex, basePath);
            processPermutationPath(currentNode, usage, allParameters, List.of(flag1, flag3, flag2), nextIndex, basePath);
            processPermutationPath(currentNode, usage, allParameters, List.of(flag2, flag1, flag3), nextIndex, basePath);
            processPermutationPath(currentNode, usage, allParameters, List.of(flag2, flag3, flag1), nextIndex, basePath);
            processPermutationPath(currentNode, usage, allParameters, List.of(flag3, flag1, flag2), nextIndex, basePath);
            processPermutationPath(currentNode, usage, allParameters, List.of(flag3, flag2, flag1), nextIndex, basePath);
        }
    }
    
    /**
     * MODIFIED METHOD: Handle large permutations for subsets
     */
    private void generateLargePermutationsForSubset(
            ParameterNode<S, ?> currentNode,
            CommandUsage<S> usage,
            List<CommandParameter<S>> allParameters,
            List<CommandParameter<S>> subset,
            int nextIndex,
            List<ParameterNode<S, ?>> basePath
    ) {
        // Create a working copy for Heap's algorithm
        final var workingSubset = new ArrayList<>(subset);
        final int n = workingSubset.size();
        final int[] indices = new int[n];
        
        // First permutation (identity)
        processPermutationPath(currentNode, usage, allParameters, new ArrayList<>(workingSubset), nextIndex, basePath);
        
        int i = 0;
        while (i < n) {
            if (indices[i] < i) {
                // Swap elements
                if (i % 2 == 0) {
                    Collections.swap(workingSubset, 0, i);
                } else {
                    Collections.swap(workingSubset, indices[i], i);
                }
                
                // Process this permutation
                processPermutationPath(currentNode, usage, allParameters, new ArrayList<>(workingSubset), nextIndex, basePath);
                
                indices[i]++;
                i = 0;
            } else {
                indices[i] = 0;
                i++;
            }
        }
    }
    
    /**
     * MODIFIED METHOD: Enhanced to mark intermediate nodes as executable when appropriate
     */
    private void processPermutationPath(
            ParameterNode<S, ?> currentNode,
            CommandUsage<S> usage,
            List<CommandParameter<S>> allParameters,
            List<CommandParameter<S>> permutation,
            int nextIndex,
            List<ParameterNode<S, ?>> basePath
    ) {
        var nodePointer = currentNode;
        final var updatedPath = new ArrayList<>(basePath);
        
        // Process each flag in the permutation
        for (int i = 0; i < permutation.size(); i++) {
            final var flagParam = permutation.get(i);
            final var flagNode = getOrCreateChildNode(nodePointer, flagParam, false);
            updatedPath.add(flagNode);
            nodePointer = flagNode;
            
            // CRITICAL FIX: Mark intermediate nodes as executable if all remaining parameters are optional
            if (areAllRemainingParametersOptional(allParameters, nextIndex) &&
                    areAllRemainingFlagsInPermutationOptional(permutation, i + 1)) {
                
                flagNode.setExecutableUsage(usage);
            }
        }
        
        // Handle continuation after all flags in this permutation
        if (nextIndex < allParameters.size()) {
            addParametersToTree(nodePointer, usage, allParameters, nextIndex, updatedPath);
        } else {
            nodePointer.setExecutableUsage(usage);
        }
    }
    
    /**
     * NEW HELPER METHOD: Check if all remaining parameters in the full parameter list are optional
     */
    private boolean areAllRemainingParametersOptional(List<CommandParameter<S>> allParameters, int startIndex) {
        for (int i = startIndex; i < allParameters.size(); i++) {
            if (!allParameters.get(i).isOptional()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * NEW HELPER METHOD: Check if all remaining flags in the current permutation are optional
     */
    private boolean areAllRemainingFlagsInPermutationOptional(List<CommandParameter<S>> permutation, int startIndex) {
        for (int i = startIndex; i < permutation.size(); i++) {
            if (!permutation.get(i).isOptional()) {
                return false;
            }
        }
        return true;
    }
    
    private ParameterNode<S, ?> getOrCreateChildNode(ParameterNode<S, ?> parent, CommandParameter<S> param, boolean onlyUnique) {
        // Optimized child lookup with early termination
        final var children = parent.getChildren();
        final String paramName = param.name();
        final Type paramType = param.valueType();
        
        for (var child : children) {
            if (child.data.name().equalsIgnoreCase(paramName) &&
                    TypeUtility.matches(child.data.valueType(), paramType)) {
                return child;
            }
        }
        
        // Create new node
        final ParameterNode<S, ?> newNode = param.isCommand()
                ? ParameterNode.createCommandNode(parent, param.asCommand(), parent.getDepth() + 1, null)
                : ParameterNode.createArgumentNode(parent, param, parent.getDepth() + 1, null);
        
        parent.addChild(newNode);
        if(onlyUnique) {
            uniqueSize++;
        }else {
            size++;
        }
        
        return newNode;
    }
    
    /**
     * Optimized contextMatch with early termination for invalid commands
     */
    @Override
    public @NotNull CommandPathSearch<S> contextMatch(
            Context<S> context,
            @NotNull ArgumentInput input
    ) {
        final var dispatch = CommandPathSearch.unknown(root);
        dispatch.append(root);
        if(!hasPermission(context.source(), root)) {
            dispatch.setResult(CommandPathSearch.Result.PAUSE);
            dispatch.setDirectUsage(root.getExecutableUsage());
            return dispatch;
        }
        
        if (input.isEmpty()) {
            var result = !hasPermission(context.source(), root) ? CommandPathSearch.Result.PAUSE : CommandPathSearch.Result.COMPLETE;
            dispatch.setResult(result);
            dispatch.setDirectUsage(root.getExecutableUsage());
            return dispatch;
        }
        
        // SAFE OPTIMIZATION: Use cached root children
        final var rootChildren = root.getChildren();
        if (rootChildren.isEmpty()) {
            return dispatch;
        }
        
        // SAFE OPTIMIZATION: Early validation for obviously invalid commands
        boolean hasMatchingChild = false;
        boolean hasOptionalChild = false;
        
        // Single pass to check both matching and optional children
        for (var child : rootChildren) {
            if (child.matchesInput(0, context)) {
                hasMatchingChild = true;
                break; // Found match, can exit early
            }
            if (child.isOptional()) {
                hasOptionalChild = true;
            }
        }
        
        // SAFE OPTIMIZATION: Fail fast for completely invalid commands
        if (!hasMatchingChild && !hasOptionalChild && !root.isGreedyParam()) {
            return dispatch; // Quick exit saves expensive tree traversal
        }
        
        // Process children efficiently
        CommandPathSearch<S> bestMatch = dispatch;
        int bestDepth = 0;
        
        for (var child : rootChildren) {
            final var result = dispatchNode(CommandPathSearch.unknown(root), context, input, child, 0);
            // Track the best (deepest) match
            if (result.getResult().isStoppable()) {
                return result; // Return immediately on complete match
            } else if (result.getLastNode() != null && result.getLastNode().getDepth() > bestDepth) {
                bestMatch = result;
                bestDepth = result.getLastNode().getDepth();
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Optimized dispatchNode with better early termination - BACK TO RECURSIVE
     */
    private @NotNull CommandPathSearch<S> dispatchNode(
            CommandPathSearch<S> commandPathSearch,
            Context<S> context,
            ArgumentInput input,
            @NotNull ParameterNode<S, ?> currentNode,
            int depth
    ) {
        final int inputSize = input.size();
        final boolean isLastDepth = (depth == inputSize - currentNode.getConsumedArguments());
        
        if (isLastDepth) {
            System.out.println("Reached last depth at node '" + currentNode.format() + "' with depth " + depth);
            return handleLastDepth(commandPathSearch, context, currentNode, depth);
        } else if (depth >= inputSize) {
            return commandPathSearch;
        }
        
        // Check permissions BEFORE processing
        if (!hasPermission(context.source(), currentNode)) {
            commandPathSearch.setResult(CommandPathSearch.Result.PAUSE);
            if (currentNode.isExecutable()) {
                commandPathSearch.setDirectUsage(currentNode.getExecutableUsage());
            }
            return commandPathSearch;
        }
        
        // Greedy parameter check
        //System.out.println("is current node '" + currentNode.format() + "' greedy? " + currentNode.isGreedyParam());
        if (currentNode.isGreedyParam()) {
            commandPathSearch.append(currentNode);
            commandPathSearch.setResult(CommandPathSearch.Result.COMPLETE);
            commandPathSearch.setDirectUsage(currentNode.getExecutableUsage());
            return commandPathSearch;
        }
        
        System.out.println("Checking if node '" + currentNode.format() + "' matches input at depth " + depth);
        boolean nodeMatches = currentNode.matchesInput(depth, context, currentNode.isOptional());
        
        if (!nodeMatches) {
            System.out.println("Node '" + currentNode.format() + "' Doesn't match input at depth " + depth);
            // Handle optional parameter skipping with proper logic
            return handleOptionalParameterSkipping(commandPathSearch, context, input, currentNode, depth);
        }
        
        // Node matches - append and continue
        commandPathSearch.append(currentNode);
        
        // Check if we can execute at this point
        if (currentNode.isExecutable() && depth == inputSize - currentNode.getConsumedArguments()) {
            commandPathSearch.setResult(CommandPathSearch.Result.COMPLETE);
            commandPathSearch.setDirectUsage(currentNode.getExecutableUsage());
            return commandPathSearch;
        }
        
        // Continue with children
        final var children = currentNode.getChildren();
        if (children.isEmpty()) {
            return commandPathSearch;
        }
        
        // Process children
        for (var child : children) {
            final var result = dispatchNode(commandPathSearch, context, input, child, depth + currentNode.getConsumedArguments());
            if (result.getResult().isStoppable()) {
                return result;
            }
        }
        
        return commandPathSearch;
    }
    
    /**
     * Optimized last depth handling
     */
    private CommandPathSearch<S> handleLastDepth(
            CommandPathSearch<S> search,
            Context<S> context,
            ParameterNode<S, ?> node,
            int depth
    ) {
        if(!node.matchesInput(depth, context)) {
            return search;
        }
        
        search.append(node);
        var result = CommandPathSearch.Result.COMPLETE;
        if (!hasPermission(context.source(), node)) {
            //we know now the node is certainly executable
            result = CommandPathSearch.Result.PAUSE;
        }
        
        if(!node.isExecutable()) {
            if (node.isCommand()) {
                search.setDirectUsage(node.data.asCommand().getDefaultUsage());
                search.setResult(result);
            }
            return search;
        }

        search.setDirectUsage(node.getExecutableUsage());
        search.setResult(result);
        
        return search;
    }
    
    /**
     * NEW: Proper optional parameter skipping logic
     */
    private CommandPathSearch<S> handleOptionalParameterSkipping(
            CommandPathSearch<S> commandPathSearch,
            Context<S> context,
            ArgumentInput input,
            ParameterNode<S, ?> currentNode,
            int depth
    ) {
        // If node is required and doesn't match, fail immediately
        if (!currentNode.isOptional()) {
            return commandPathSearch;
        }
        
        
        // First, try to execute at current node if possible (optional parameter not provided)
        if (currentNode.isExecutable()) {
            commandPathSearch.append(currentNode);
            commandPathSearch.setResult(CommandPathSearch.Result.COMPLETE);
            commandPathSearch.setDirectUsage(currentNode.getExecutableUsage());
            return commandPathSearch;
        }
        
        // For optional parameters that don't match, try to skip to next parameter
        final var children = currentNode.getChildren();
        
        for (var child : children) {
            if (!hasPermission(context.source(), child)) {
                continue; // Skip children without permission
            }
            
            if (child.matchesInput(depth, context)) {
                // Found a matching child, continue with it
                return dispatchNode(commandPathSearch, context, input, child, depth);
            }
        }
        
        return commandPathSearch;
    }
    
  
    @Override
    public HelpEntryList<S> queryHelp(@NotNull HelpQuery<S> query) {
        final HelpEntryList<S> results = new HelpEntryList<>();
        
        if (query.getLimit() <= 0) {
            return HelpEntryList.empty();
        }
        
        collectHelpEntries(uniqueRoot, query, results);
        return results;
    }
    
    /**
     * Collects help entries in deep hierarchical mode - full tree traversal with structure
     */
    private void collectHelpEntries(
            ParameterNode<S, ?> node,
            HelpQuery<S> query,
            HelpEntryList<S> results
    ) {
        // Check depth limit using node's depth
        if (node.getDepth() > query.getMaxDepth()) {
            return;
        }
        
        // Check result limit
        if (results.size() >= query.getLimit()) {
            return;
        }
        // Apply filters to current node
        if (!passesFilters(node, query.getFilters())) {
            return;
        }
        
        // Add current node ONLY if it has executableUsage (truly executable)
        if (node.isExecutable()) {
            if(!node.isRoot() || /*Root Node :D*/ query.getRootUsagePredicate().test(node.getExecutableUsage())) {
                results.add(helpEntryFactory.createEntry(node));
            }
        }
        
        // Recursively process children (DFS traversal) - continues even through command nodes
        for (var child : node.getChildren()) {
            collectHelpEntries(child, query, results);
        }
    }
    
    /**
     * Applies all filters to a node
     * Short-circuits on first failed filter for efficiency
     */
    private boolean passesFilters(ParameterNode<S, ?> node, Queue<HelpFilter<S>> filters) {
        for (HelpFilter<S> filter : filters) {
            if (!filter.filter(node)) {
                return false;
            }
        }
        return true;
    }
    
    
    @Override
    public @NotNull List<String> tabComplete(@NotNull SuggestionContext<S> context) {
        List<String> results = new ArrayList<>(INITIAL_SUGGESTIONS_CAPACITY);
        final String prefix = context.getArgToComplete().value();
        final boolean hasPrefix = prefix != null && !prefix.isBlank();
        
        return tabComplete$1(context, prefix, hasPrefix, results);
    }
    
    
    @SuppressWarnings("unused")
    private List<String> tabComplete$1(
            SuggestionContext<S> context,
            String prefix,
            boolean hasPrefix,
            List<String> results
    ) {
       
        if(!hasAutoCompletionPermission(context.source(), uniqueRoot)) {
            return Collections.emptyList();
        }
        
        for(var child : uniqueRoot.getChildren()) {
            tabCompleteNode(child, context, 0, results);
        }
        return results.stream()
                .filter((suggestion)-> !hasPrefix || fastStartsWith(suggestion, prefix))
                .toList();
    }
    
    private void tabCompleteNode(
            final ParameterNode<S, ?> node,
            final SuggestionContext<S> context,
            int inputDepth,
            final List<String> results
    ) {
        
        int lastIndex = context.getArgToComplete().index();
        if(inputDepth > lastIndex) {
            return;
        }
        
        if(inputDepth == lastIndex) {
            results.addAll(getResolverCached(node.data).autoComplete(context, node.data));
            if(imperatConfig.isOptionalParameterSuggestionOverlappingEnabled() && node.isOptional() && !(node.isTrueFlag()) ) {
                collectOverlappingSuggestions(node, node, context, results);
            }
        }else {
            String currentInput = context.arguments().getOr(inputDepth, null);
            assert currentInput != null;
            if(!hasAutoCompletionPermission(context.source(), node)) {
                //System.out.println("NO PERM");
                return;
            }
            
            if(node.isGreedyParam()) {
                tabCompleteNode(node, context, lastIndex, results);
                return;
            }
            
            if (!node.matchesInput(inputDepth, context, node.isOptional()) && node.isRequired()) {
                return;
            }
            
            for (var child : node.getChildren()) {
                tabCompleteNode(child, context, inputDepth+node.getConsumedArguments(), results);
            }
            
        }
    }
    
    /**
     * Recursively collect suggestions from subsequent parameters with different types
     * Stops at required parameters (inclusive)
     */
    private void collectOverlappingSuggestions(
            ParameterNode<S, ?> origin,
            ParameterNode<S, ?> currentNode,
            SuggestionContext<S> context,
            List<String> results
    ) {
        for (var nextNode : currentNode.getChildren()) {
            // Skip if same type as current node
            if (nextNode.data.valueType().equals(origin.data.valueType())) {
                //System.out.println("Skipping " + nextNode.format() + " due to having same type as ");
                continue;
            }
            
            // Check permissions
            if ( !(nextNode.isCommand() && nextNode.data.asCommand().isIgnoringACPerms() )
                    && !hasPermission(context.source(), nextNode)) {
                //System.out.println("Skipping " + nextNode.format() + " due to having no perm for it");
                continue;
            }
            
            // Collect suggestions from this node
            //System.out.println("Getting from node " + nextNode.format() + "'s overlap:");
            final var resolver = getResolverCached(nextNode.data);
            final var suggestions = resolver.autoComplete(context, nextNode.data);
            if (suggestions != null && !suggestions.isEmpty()) {
                results.addAll(suggestions);
                //System.out.println("Fetched '" + String.join(",", suggestions)  +"'");
            }
            
            // If this is a required parameter, stop here (it's a stop point)
            if (!nextNode.isOptional()) {
                //System.out.println("Skipping non-optional node " + nextNode.format());
                continue; // Don't traverse deeper from this branch
            }
            
            // If it's optional, continue recursively
            collectOverlappingSuggestions(origin, nextNode, context, results);
        }
    }
    
    
    private boolean hasPermission(S source, ParameterNode<S, ?> node) {
        return permissionChecker.hasPermission(source, node.getPermission());
    }
    
    private boolean hasAutoCompletionPermission(S src, ParameterNode<S, ?> node) {
        if(node.isCommand() && node.getData().asCommand().isIgnoringACPerms()) {
            return true;
        }
        return hasPermission(src, node);
    }
    
    /**
     * CACHED resolver lookup - Eliminates config lookups
     */
    private SuggestionResolver<S> getResolverCached(CommandParameter<S> param) {
        return imperatConfig.getParameterSuggestionResolver(param);
    }
    
    /**
     * prefix matching - Optimized for tab completion
     */
    private static boolean fastStartsWith(String str, String prefix) {
        final int prefixLen = prefix.length();
        if (str.length() < prefixLen) return false;
        
        // Optimized for very short prefixes (common in tab completion)
        if (prefixLen <= 3) {
            for (int i = 0; i < prefixLen; i++) {
                if (Character.toLowerCase(str.charAt(i)) != Character.toLowerCase(prefix.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        
        // For longer prefixes, use regionMatches
        return str.regionMatches(true, 0, prefix, 0, prefixLen);
    }
    
    /**
     * Searches for closest usage to a context entered.
     * returns an ORDERED/SORTED set of unique {@link CommandUsage} , ordered by
     * how close they are to the context entered, the closest usage to the context entered shall be
     * placed on top of this ordered set of closest usages.
     *
     * @param context the context to search with.
     * @return the closest usages ordered by how close they are to a {@link Context}
     */
    @Override
    public Set<CommandUsage<S>> getClosestUsages(Context<S> context) {
        final var queue = context.arguments();
        final String firstArg = queue.getOr(0, null);
        
        final var startingNode = (firstArg == null) ? root : findStartingNode(context, root);
        
        return (startingNode == null)
                ? Set.of(rootCommand.getDefaultUsage())
                : getClosestUsagesRecursively(new LinkedHashSet<>(), startingNode, context);
    }
    
    private ParameterNode<S, ?> findStartingNode(Context<S> context, ParameterNode<S, ?> root) {
        for (var child : root.getChildren()) {
            if (child.matchesInput(child.getDepth(), context)) {
                return child;
            }
        }
        return null;
    }
    
    private Set<CommandUsage<S>> getClosestUsagesRecursively(
            Set<CommandUsage<S>> currentUsages,
            ParameterNode<S, ?> node,
            Context<S> context
    ) {
        if (node.isExecutable()) {
            final var usage = node.getExecutableUsage();
            currentUsages.add(usage);
        }
        
        if (!node.isLast()) {
            final var children = node.getChildren();
            final var arguments = context.arguments();
            
            for (var child : children) {
                final String correspondingInput = arguments.getOr(child.getDepth(), null);
                
                if (correspondingInput == null) {
                    if (child.isRequired()) {
                        addPermittedUsages(currentUsages, child, context);
                    }
                } else if (child.matchesInput(child.getDepth(), context)) {
                    addPermittedUsages(currentUsages, child, context);
                }
            }
        }
        
        return currentUsages;
    }
    
    private void addPermittedUsages(
            Set<CommandUsage<S>> currentUsages,
            ParameterNode<S, ?> child,
            Context<S> context
    ) {
        final var childUsages = getClosestUsagesRecursively(new LinkedHashSet<>(), child, context);
        currentUsages.addAll(childUsages);
    }
    
}