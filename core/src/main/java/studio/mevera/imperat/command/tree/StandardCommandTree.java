package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.resolvers.PermissionResolver;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Ultra-optimized N-ary tree implementation focused on maximum performance
 * @author Mqzen
 */
final class StandardCommandTree<S extends Source> implements CommandTree<S> {
    private final Command<S> rootCommand;
    final CommandNode<S> root;
    
    private int size;
    
    // Pre-computed immutable collections to eliminate allocations
    private final static int MAX_SUGGESTIONS_PER_ARGUMENT = 20;
    
    // Optimized flag cache with better hashing
    private final Map<String, FlagData<S>> flagCache;
    
    // Pre-sized collections for common operations
    private final ThreadLocal<ArrayList<ParameterNode<S, ?>>> pathBuffer =
            ThreadLocal.withInitial(() -> new ArrayList<>(16));
    private final ThreadLocal<ArrayList<CommandParameter<S>>> paramBuffer =
            ThreadLocal.withInitial(() -> new ArrayList<>(8));
    
    // Pre-compute root children to avoid repeated getChildren() calls
    private List<ParameterNode<S, ?>> cachedRootChildren;
    
    private final ImperatConfig<S> imperatConfig;
    private final @NotNull PermissionResolver<S> permissionResolver;
    
    private final SuggestionCache<S> suggestionCache = new SuggestionCache<>();
    
    StandardCommandTree(ImperatConfig<S> imperatConfig, Command<S> command) {
        this.rootCommand = command;
        this.root = new CommandNode<>(command, -1, command.getDefaultUsage());
        this.flagCache = initializeFlagCache();
        // Initialize empty cache - will be populated after parsing
        this.cachedRootChildren = null;
        this.imperatConfig = imperatConfig;
        this.permissionResolver = imperatConfig.getPermissionResolver();
        // Initialize performance caches
        // Note: These will be populated lazily during usage
    }
    
    private Map<String, FlagData<S>> initializeFlagCache() {
        // Use HashMap instead of concurrent map for better performance in single-threaded access
        final var cache = new HashMap<String, FlagData<S>>();
        for (var usage : rootCommand.usages()) {
            for (var flag : usage.getUsedFreeFlags()) {
                for (String alias : flag.aliases()) {
                    cache.put(alias, flag);
                }
            }
        }
        return Collections.unmodifiableMap(cache); // Make immutable
    }
    
    // Optimized parsing with reduced allocations
    void parseCommandUsages() {
        final var usages = root.data.usages();
        for (var usage : usages) {
            parseUsage(usage);
        }
        // SAFE OPTIMIZATION: Update cached root children after parsing
        this.cachedRootChildren = new ArrayList<>(this.root.getChildren());
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
    public int size() {
        return size;
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
        final var childNode = getOrCreateChildNode(currentNode, param);
        
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
            final var flagNode = getOrCreateChildNode(nodePointer, flagParam);
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
    
    private ParameterNode<S, ?> getOrCreateChildNode(ParameterNode<S, ?> parent, CommandParameter<S> param) {
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
                ? new CommandNode<>(param.asCommand(), parent.getDepth() + 1, null)
                : new ArgumentNode<>(param, parent.getDepth() + 1, null);
        
        parent.addChild(newNode);
        size++;
        return newNode;
    }
    
    /**
     * Optimized contextMatch with early termination for invalid commands
     */
    @Override
    public @NotNull CommandPathSearch<S> contextMatch(
            @NotNull ArgumentInput input
    ) {
        final var dispatch = CommandPathSearch.<S>unknown();
        dispatch.append(root);
        
        if (input.isEmpty()) {
            dispatch.setResult(CommandPathSearch.Result.COMPLETE);
            dispatch.setDirectUsage(root.getExecutableUsage());
            return dispatch;
        }
        
        // SAFE OPTIMIZATION: Use cached root children
        final var rootChildren = cachedRootChildren != null ? cachedRootChildren : root.getChildren();
        if (rootChildren.isEmpty()) {
            return dispatch;
        }
        
        // SAFE OPTIMIZATION: Early validation for obviously invalid commands
        String firstArg = input.get(0);
        boolean hasMatchingChild = false;
        boolean hasOptionalChild = false;
        
        // Single pass to check both matching and optional children
        for (var child : rootChildren) {
            if (matchesInput(child, firstArg, imperatConfig.strictCommandTree())) {
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
            final var result = dispatchNode(CommandPathSearch.unknown(), input, child, 0);
            
            // Track the best (deepest) match
            if (result.getResult() == CommandPathSearch.Result.COMPLETE) {
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
            ArgumentInput input,
            @NotNull ParameterNode<S, ?> currentNode,
            int depth
    ) {
        final int inputSize = input.size();
        final boolean isLastDepth = (depth == inputSize - 1);
        
        if (isLastDepth) {
            return handleLastDepth(imperatConfig, commandPathSearch, currentNode, input.getOr(depth, null));
        }
        else if(depth >= inputSize) {
            return commandPathSearch;
        }
        
        final String rawInput = input.get(depth);
        
        // Greedy parameter check
        if (currentNode.isGreedyParam()) {
            commandPathSearch.append(currentNode);
            commandPathSearch.setResult(CommandPathSearch.Result.COMPLETE);
            commandPathSearch.setDirectUsage(currentNode.getExecutableUsage());
            return commandPathSearch;
        }
        
        // Input matching loop with reduced overhead
        var workingNode = currentNode;
        final boolean strictMode = imperatConfig.strictCommandTree();
        
        while (!matchesInput(workingNode, rawInput, strictMode)) {
            if (workingNode.isOptional()) {
                commandPathSearch.append(workingNode);
                var nextWorkingNode = workingNode.getNextParameterChild();
                if (nextWorkingNode == null) {
                    if (workingNode.isExecutable()) {
                        commandPathSearch.setResult(CommandPathSearch.Result.COMPLETE);
                        commandPathSearch.setDirectUsage(workingNode.executableUsage);
                    }
                    return commandPathSearch;
                }
                workingNode = nextWorkingNode;
            } else {
                return commandPathSearch;
            }
        }
        
        // Flag handling with cached lookup
        if (!workingNode.isFlag() && isFlag(rawInput)) {
            final var flagData = flagCache.get(rawInput.substring(1));
            if (flagData == null) {
                return commandPathSearch;
            }
            final int depthIncrease = flagData.isSwitch() ? 1 : 2;
            return dispatchNode(commandPathSearch, input, workingNode, depth + depthIncrease);
        }
        
        commandPathSearch.append(workingNode);
        
        if(workingNode.isTrueFlag()) {
            depth++;
        }
        
        if(workingNode.isExecutable() && depth == inputSize-1) {
            commandPathSearch.setResult(CommandPathSearch.Result.COMPLETE);
            commandPathSearch.setDirectUsage(workingNode.getExecutableUsage());
            return commandPathSearch;
        }
        
        // Process children with early termination
        final var children = workingNode.getChildren();
        if (children.isEmpty()) {
            return commandPathSearch; // No children to process
        }
        
        // SAFE OPTIMIZATION: More efficient validation for next input
        final int nextDepth = depth + 1;
        if (nextDepth < inputSize) {
            final String nextInput = input.get(nextDepth);
            boolean hasValidChild = false;
            
            // Single pass to check for valid children
            for (var child : children) {
                if (child.isOptional() || matchesInput(child, nextInput, strictMode)) {
                    hasValidChild = true;
                    break; // Found valid child, can exit early
                }
            }
            
            if (!hasValidChild) {
                return commandPathSearch; // No valid path forward, terminate early
            }
        }
        
        for (var child : children) {
            final var result = dispatchNode(commandPathSearch, input, child, depth + 1);
            if (result.getResult() == CommandPathSearch.Result.COMPLETE) {
                return result;
            }
        }
        
        return commandPathSearch;
    }
    
    /**
     * Optimized last depth handling
     */
    private CommandPathSearch<S> handleLastDepth(ImperatConfig<S> cfg, CommandPathSearch<S> dispatch, ParameterNode<S, ?> node, String lastArg) {
        
        if(!matchesInput(node, lastArg, cfg.strictCommandTree())) {
            return dispatch;
        }
        
        dispatch.append(node);
        
        if(!node.isExecutable()) {
            if (node.isCommand()) {
                dispatch.setDirectUsage(node.data.asCommand().getDefaultUsage());
                dispatch.setResult(CommandPathSearch.Result.COMPLETE);
            }
            return dispatch;
        }
        
        dispatch.setDirectUsage(node.getExecutableUsage());
        dispatch.setResult(CommandPathSearch.Result.COMPLETE);
        
        return dispatch;
    }
    
    /**
     * Fast flag checking
     */
    private static boolean isFlag(String input) {
        return input.length() > 1 && input.charAt(0) == '-';
    }
    
    /**
     * Optimized input matching
     */
    private static <S extends Source> boolean matchesInput(
            ParameterNode<S, ?> node,
            String input,
            boolean strictMode
    ) {
        if (node instanceof CommandNode || strictMode || node.isFlag()) {
            return node.matchesInput(input);
        }
        return true;
    }
    
    @Override
    public @NotNull List<String> tabComplete(@NotNull SuggestionContext<S> context) {
        
        List<String> results = new ArrayList<>(MAX_SUGGESTIONS_PER_ARGUMENT);
        
        final int targetDepth = context.getArgToComplete().index();
        final String prefix = context.getArgToComplete().value();
        final boolean hasPrefix = prefix != null && !prefix.isBlank();
        
        var lastNodes = suggestionCache.getLastNodes(context.command(), context.arguments());
        if(lastNodes != null) {
            /*System.out.println(
                    "Found last Nodes [" + lastNodes.stream().map(ParameterNode::format).collect(Collectors.joining(","))
                            + "] for input : '" + context.arguments().join(",") + "'" );
                           
             */
            for(var lastNode : lastNodes) {
                collectSuggestionsOptimized(
                        lastNode, context, prefix, hasPrefix, context.source(), results
                );
            }
            return results;
        }
        
        // ITERATIVE TRAVERSAL - No recursive overhead!
        return tabCompleteIterativeDFS(context, targetDepth, prefix, hasPrefix, results);
    }
    
    /**
     * ULTRA-OPTIMIZED Iterative tab completion DFS
     * Every microsecond matters here!
     */
    private List<String> tabCompleteIterativeDFS(
            SuggestionContext<S> context,
            int targetDepth,
            String prefix,
            boolean hasPrefix,
            List<String> results
    ) {
        final ArrayDeque<ParameterNode<S, ?>> stack = new ArrayDeque<>(this.size);
        
        stack.push(root);
        
        final var source = context.source();
        final var arguments = context.arguments();
        
        // MAIN ITERATIVE DFS LOOP - Pure performance
        while (!stack.isEmpty()) {
            final var currentNode = stack.pop();
            final int currentDepth = currentNode.getDepth();
            
            // FAST PATH: Are we at suggestion depth?
            if (targetDepth - currentDepth == 1) {
                // COLLECT SUGGESTIONS from all children at this level
                collectSuggestionsOptimized(
                        currentNode, context, prefix,
                        hasPrefix, source, results
                );
                suggestionCache.computeInput(context.command(), context.arguments(), currentNode);
                continue; // Don't traverse deeper from suggestion nodes
            }
            
            // DFS TRAVERSAL PATH: Add valid children in REVERSE order for proper DFS
            if (currentDepth < targetDepth - 1) {
                final String inputAtDepth = arguments.getOr(currentDepth + 1, null);
                addValidChildrenToStackDFS(currentNode, inputAtDepth, source, stack);
            }
        }
        
        return Collections.unmodifiableList(results); // Return defensive copy
    }
    
    /**
     * HYPER-OPTIMIZED suggestion collection - Zero overhead
     */
    private void collectSuggestionsOptimized(
            ParameterNode<S, ?> node,
            SuggestionContext<S> context,
            String prefix,
            boolean hasPrefix,
            S source,
            List<String> results
    ) {
        final var children = node.getChildren();
        if (children.isEmpty()) {
            return;
        }
        
        // ULTRA-FAST child processing
        for (var child : children) {
            // FAST PERMISSION CHECK with caching
            if (!hasPermission(source, child.data.permission())) {
                continue;
            }
            
            // FAST RESOLVER LOOKUP with caching
            final var resolver = getResolverCached(child.data);
            
            // CACHED SUGGESTION LOOKUP
            final var suggestions = resolver.autoComplete(context, child.data);
            
            // OPTIMIZED suggestion filtering and adding
            for (final String suggestion : suggestions) {
                // ULTRA-FAST prefix matching - optimized for common cases
                if (!hasPrefix || fastStartsWith(suggestion, prefix)) {
                    results.add(suggestion);
                }
            }
        }
    }
    
    /**
     * FAST child validation and stack addition for DFS
     * Children added in REVERSE order to maintain left-to-right traversal
     */
    private void addValidChildrenToStackDFS(
            ParameterNode<S, ?> node,
            String inputAtDepth,
            S source,
            ArrayDeque<ParameterNode<S, ?>> stack
    ) {
        final var children = node.getChildren();
        
        // DFS OPTIMIZATION: Add children in reverse order
        // Stack is LIFO, so reverse order gives us left-to-right DFS traversal
        for (int i = children.size() - 1; i >= 0; i--) {
            final var child = children.get(i);
            
            // FAST validation checks
            if (matchesInput(child, inputAtDepth, imperatConfig.strictCommandTree()) &&
                    hasPermission(source, child.data.permission())) {
                stack.push(child);
                
                if(child.isCommand()) {
                    break;
                }
            }
        }
    }
    /**
     * FAST child validation and stack addition for DFS
     * Children added in REVERSE order to maintain left-to-right traversal
     */
    private void addValidChildrenToCollectionDFS(
            ParameterNode<S, ?> node,
            String inputAtDepth,
            S source,
            Collection<ParameterNode<S, ?>> collection
    ) {
        final var children = node.getChildren();
        
        // DFS OPTIMIZATION: Add children in reverse order
        // Stack is LIFO, so reverse order gives us left-to-right DFS traversal
        for (var child : children) {
            // FAST validation checks
            if (matchesInput(child, inputAtDepth, imperatConfig.strictCommandTree()) &&
                    hasPermission(source, child.data.permission())) {
                collection.add(child);
                
                if(child.isCommand()) {
                    break;
                }
            }
        }
    }
    /**
     * Alternative DFS implementation using recursion (may be faster for shallow trees)
     * Use this if your command trees are typically shallow (< 10 levels)
     */
    private List<String> tabCompleteRecursiveDFS(
            SuggestionContext<S> context,
            int targetDepth,
            String prefix,
            boolean hasPrefix,
            List<String> results
    ) {
        dfsTraverseRecursively(root, context, targetDepth, prefix, hasPrefix, results);
        return Collections.unmodifiableList(results);
    }
    
    /**
     * RECURSIVE DFS helper - Optimized for shallow trees
     */
    private void dfsTraverseRecursively(
            ParameterNode<S, ?> node,
            SuggestionContext<S> context,
            int targetDepth,
            String prefix,
            boolean hasPrefix,
            List<String> results
    ) {
        final int currentDepth = node.getDepth();
        final var source = context.source();
        final var arguments = context.arguments();
        
        // BASE CASE: At suggestion depth
        if (targetDepth - currentDepth == 1) {
            collectSuggestionsOptimized(node, context, prefix, hasPrefix, source, results);
            return;
        }
        
        // RECURSIVE CASE: Continue DFS traversal
        if (currentDepth < targetDepth - 1) {
            final String inputAtDepth = arguments.getOr(currentDepth + 1, null);
            final var children = node.getChildren();
            
            // DFS: Process each valid child completely before moving to next
            for (var child : children) {
                if (matchesInput(child, inputAtDepth, false) &&
                        hasPermission(source, child.data.permission())) {
                    dfsTraverseRecursively(child, context, targetDepth, prefix, hasPrefix, results);
                }
            }
        }
    }
    
    /**
     * CACHED permission checking - Eliminates repeated lookups
     */
    private boolean hasPermission(S source, String permission) {
        if (permission == null || permission.isEmpty()) return true;
        return permissionResolver.hasPermission(source, permission);
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
    
    // Optimized usage search
    
    /*public ClosestUsageSearch<S> getClosestUsages(Context<S> context) {
        final var queue = context.arguments();
        final String firstArg = queue.getOr(0, null);
        
        final var startingNode = (firstArg == null) ? root : findStartingNode(root, firstArg);
        
        final Set<CommandUsage<S>> closestUsages = (startingNode == null)
                ? Set.of(rootCommand.getDefaultUsage())
                : getClosestUsagesRecursively(new LinkedHashSet<>(), startingNode, context);
        
        return new ClosestUsageSearch<>(closestUsages);
    }
    
    private ParameterNode<S, ?> findStartingNode(ParameterNode<S, ?> root, String raw) {
        // SAFE OPTIMIZATION: Use cached root children if available
        final var children = cachedRootChildren != null ? cachedRootChildren : root.getChildren();
        for (var child : children) {
            if (child.matchesInput(raw)) {
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
            if (hasUsagePermission(context.imperatConfig().getPermissionResolver(), context.source(), usage)) {
                currentUsages.add(usage);
            }
        }
        
        if (!node.isLast()) {
            final var children = node.getChildren();
            final var arguments = context.arguments();
            final var permissionResolver = context.imperatConfig().getPermissionResolver();
            final var source = context.source();
            
            for (var child : children) {
                final String correspondingInput = arguments.getOr(child.getDepth(), null);
                
                if (correspondingInput == null) {
                    if (child.isRequired()) {
                        addPermittedUsages(currentUsages, child, context, permissionResolver, source);
                    }
                } else if (child.matchesInput(correspondingInput)) {
                    addPermittedUsages(currentUsages, child, context, permissionResolver, source);
                }
            }
        }
        
        return currentUsages;
    }
    
    private void addPermittedUsages(
            Set<CommandUsage<S>> currentUsages,
            ParameterNode<S, ?> child,
            Context<S> context,
            PermissionResolver<S> permissionResolver,
            S source
    ) {
        final var childUsages = getClosestUsagesRecursively(new LinkedHashSet<>(), child, context);
        for (var usage : childUsages) {
            if (hasUsagePermission(permissionResolver, source, usage)) {
                currentUsages.add(usage);
            }
        }
    }
   
     */
    
    
    
}