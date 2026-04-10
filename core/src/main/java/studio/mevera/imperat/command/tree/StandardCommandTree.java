package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.tree.help.HelpEntryFactory;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpFilter;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.permissions.PermissionHolder;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Pair;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeUtility;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * N-ary tree implementation focused on maximum performance
 * @author Mqzen
 */
final class StandardCommandTree<S extends CommandSource> implements CommandTree<S> {

    // Pre-computed immutable collections to eliminate allocations
    private final static int INITIAL_SUGGESTIONS_CAPACITY = 20;
    final LiteralCommandNode<S> root;
    private final Command<S> rootCommand;
    // Pre-sized collections for common operations
    private final ThreadLocal<ArrayList<CommandNode<S, ?>>> pathBuffer =
            ThreadLocal.withInitial(() -> new ArrayList<>(16));

    private final ImperatConfig<S> imperatConfig;
    private final @NotNull PermissionChecker<S> permissionChecker;
    private final HelpEntryFactory<S> helpEntryFactory = HelpEntryFactory.defaultFactory();
    private boolean nodeCachesDirty;
    private boolean rootHasDedicatedDefaultPathway;
    int size;

    StandardCommandTree(ImperatConfig<S> imperatConfig, Command<S> command) {
        this.rootCommand = command;
        this.root = CommandNode.createCommandNode(null, command, -1, command.getDefaultPathway());
        this.imperatConfig = imperatConfig;
        this.permissionChecker = imperatConfig.getPermissionChecker();
        refreshNodeCaches();
    }



    @Override
    public @NotNull Command<S> root() {
        return rootCommand;
    }

    @Override
    public @NotNull LiteralCommandNode<S> rootNode() {
        return root;
    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public void parseUsage(@NotNull CommandPathway<S> usage) {
        // Register flags once

        final var parameters = usage.getArguments();
        if (usage.isDefault()) {
            root.setExecutableUsage(usage);
            markNodeCachesDirty();
            return;
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

        markNodeCachesDirty();

    }

    @Override
    public void parseSubTree(@NotNull CommandTree<S> subTree, String attachmentNode) {
        CommandNode<S, ?> attachTo;
        if (attachmentNode.isBlank()) {
            attachTo = root;
        } else {
            attachTo = findMatchingNode(root, attachmentNode);
        }

        if (attachTo == null) {
            return;
        }

        var subRoot = subTree.rootNode();
        attachTo.addChild(subRoot);

        // Build the full ancestor prefix by walking UP from the attachment node
        // to the root (exclusive), then append the subcommand literal itself.
        List<Argument<S>> fullPrefix = collectAncestorArgs(attachTo);
        if (subRoot.isLiteral()) {
            fullPrefix.add(subRoot.getData());
        }

        // Merge pathways in the subtree, prepending the full prefix
        mergePathwaysInSubTree(subRoot, fullPrefix);
        markNodeCachesDirty();
    }

    /**
     * Walks UP from the given node to the root (exclusive), collecting
     * all node data (arguments and literals) in top-down order.
     */
    private List<Argument<S>> collectAncestorArgs(@NotNull CommandNode<S, ?> node) {
        List<Argument<S>> ancestors = new ArrayList<>();
        CommandNode<S, ?> current = node;
        while (current != null && !current.isRoot()) {
            ancestors.add(0, current.getData());
            current = current.getParent();
        }
        return ancestors;
    }

    /**
     * Recursively walks every node in the sub-tree. For each executable node,
     * creates a merged pathway: {@code externalPrefix + internalPath + personalArgs}.
     * <p>
     * - externalPrefix: the ancestor path from the parent tree's root to the subtree root (inclusive)
     * - internalPath: the path from the subtree root (exclusive) to the current node (inclusive)
     * - personalArgs: trailing non-command args from the original pathway that extend
     *   beyond this node (e.g., optional children not yet in the tree)
     * <p>
     * {@code internalPath} is built up during recursion by appending each child's data.
     */
    private void mergePathwaysInSubTree(
            @NotNull CommandNode<S, ?> node,
            @NotNull List<Argument<S>> externalPrefix
    ) {
        // Start recursion from the subtree root with an empty internal path
        // (the subtree root itself is already in the externalPrefix)
        for (var child : node.getChildren()) {
            List<Argument<S>> internalPath = new ArrayList<>();
            internalPath.add(child.getData());
            mergeNodeRecursive(child, externalPrefix, internalPath);
        }
        // Also handle the subtree root itself if executable
        if (node.isExecutable() && node.getExecutableUsage() != null) {
            CommandPathway<S> original = node.getExecutableUsage();
            List<Argument<S>> personalArgs = extractPersonalArgs(original);
            List<Argument<S>> mergedArgs = new ArrayList<>(externalPrefix.size() + personalArgs.size());
            mergedArgs.addAll(externalPrefix);
            mergedArgs.addAll(personalArgs);
            node.setExecutableUsage(buildMergedPathway(original, mergedArgs, externalPrefix));
        }
    }

    private void mergeNodeRecursive(
            @NotNull CommandNode<S, ?> node,
            @NotNull List<Argument<S>> externalPrefix,
            @NotNull List<Argument<S>> internalPath
    ) {
        if (node.isExecutable() && node.getExecutableUsage() != null) {
            CommandPathway<S> original = node.getExecutableUsage();
            List<Argument<S>> personalArgs = extractPersonalArgs(original);

            // Build: externalPrefix + internalPath + trailingArgs
            // where trailingArgs = personal args that come AFTER this node's position
            List<Argument<S>> mergedArgs = new ArrayList<>(externalPrefix.size() + internalPath.size() + personalArgs.size());
            mergedArgs.addAll(externalPrefix);
            mergedArgs.addAll(internalPath);

            // Find trailing personal args: those in the original pathway AFTER
            // the current node's own argument
            String nodeName = node.getData().getName();
            List<Argument<S>> trailingArgs = List.of();
            for (int i = 0; i < personalArgs.size(); i++) {
                if (personalArgs.get(i).getName().equalsIgnoreCase(nodeName)) {
                    if (i + 1 < personalArgs.size()) {
                        trailingArgs = personalArgs.subList(i + 1, personalArgs.size());
                    }
                    break;
                }
            }
            mergedArgs.addAll(trailingArgs);

            node.setExecutableUsage(buildMergedPathway(original, mergedArgs, externalPrefix));
        }

        for (var child : node.getChildren()) {
            List<Argument<S>> extendedInternal = new ArrayList<>(internalPath);
            extendedInternal.add(child.getData());
            mergeNodeRecursive(child, externalPrefix, extendedInternal);
        }
    }

    /**
     * Extracts the "personal" (non-prefix) arguments from a pathway.
     * The personal args are everything after the last command-type argument.
     * If the pathway has no command args, all args are personal.
     */
    private List<Argument<S>> extractPersonalArgs(CommandPathway<S> pathway) {
        List<Argument<S>> args = pathway.getArguments();
        if (args.isEmpty()) {
            return args;
        }

        int lastCommandIndex = -1;
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).isCommand()) {
                lastCommandIndex = i;
            }
        }

        if (lastCommandIndex < 0) {
            return args; // No command args — all are personal
        }

        if (lastCommandIndex + 1 >= args.size()) {
            return List.of(); // Only command args, no personal params
        }
        return args.subList(lastCommandIndex + 1, args.size());
    }

    /**
     * Builds a new merged CommandPathway copying execution, permissions, flags, etc.
     */
    private CommandPathway<S> buildMergedPathway(
            @NotNull CommandPathway<S> original,
            @NotNull List<Argument<S>> mergedArgs,
            @NotNull List<Argument<S>> prefix
    ) {
        var builder = CommandPathway.<S>builder(original.getMethodElement())
                              .arguments(mergedArgs)
                              .execute(original.getExecution())
                              .permission(original.getPermissionsData())
                              .description(original.getDescription())
                              .coordinator(original.getCoordinator())
                              .cooldown(original.getCooldown())
                              .examples(original.getExamples());

        Command<S> owningCommand = findOwningCommandFromPath(prefix);
        var pathway = builder.build(owningCommand);

        for (var flag : original.getFlagExtractor().getRegisteredFlags()) {
            pathway.addFlag(flag);
        }
        return pathway;
    }

    /**
     * Finds the owning command from a prefix path.
     */
    private Command<S> findOwningCommandFromPath(List<Argument<S>> path) {
        for (int i = path.size() - 1; i >= 0; i--) {
            if (path.get(i).isCommand()) {
                Command<S> sub = path.get(i).asCommand();
                Command<S> parent = sub.getParent();
                if (parent != null) {
                    return parent;
                }
            }
        }
        return rootCommand;
    }

    /**
     * Finds the node representing the last parameter of a pathway.
     * This is where subcommands should attach.
     */
    private @Nullable CommandNode<S, ?> findMatchingNode(
            LiteralCommandNode<S> root,
            String attachmentNode
    ) {
        return root.findNode((n) -> n.format().equals(attachmentNode));
    }

    boolean isLastRequiredNode(CommandNode<S, ?> node) {
        return node.isRequired() && node.getChildren().stream().noneMatch(CommandNode::isRequired);
    }

    private void addParametersToTree(
            CommandNode<S, ?> currentNode,
            CommandPathway<S> usage,
            List<Argument<S>> parameters,
            int index,
            List<CommandNode<S, ?>> path
    ) {
        final int paramSize = parameters.size();
        if (index >= paramSize) {
            currentNode.setExecutableUsage(usage);
            return;
        }

        // Regular parameter handling — create child FIRST
        final var param = parameters.get(index);
        final var childNode = getOrCreateChildNode(currentNode, param);

        // NOW check if currentNode is a greedy param or the last required node
        // (after child creation so the child is visible to isLastRequiredNode)
        if (currentNode.isGreedyParam() || isLastRequiredNode(currentNode)) {
            currentNode.setExecutableUsage(usage);
        }

        // Efficient path management
        final int pathSize = path.size();
        path.add(childNode);

        try {
            addParametersToTree(childNode, usage, parameters, index + 1, path);
        } finally {
            // Restore path size efficiently
            if (path.size() > pathSize) {
                path.remove(pathSize);
            }
        }
    }


    private CommandNode<S, ?> getOrCreateChildNode(CommandNode<S, ?> parent, Argument<S> arg) {
        // Optimized child lookup with early termination
        final var children = parent.getChildren();
        final String paramName = arg.getName();
        final Type paramType = arg.valueType();

        for (var child : children) {
            if (child.data.getName().equalsIgnoreCase(paramName) &&
                        TypeUtility.matches(child.data.valueType(), paramType)) {
                return child;
            }
        }

        // Create new node
        final CommandNode<S, ?> newNode = arg.isCommand()
                                                  ? CommandNode.createCommandNode(parent, arg.asCommand(), parent.getDepth() + 1, null)
                                                  : CommandNode.createArgumentNode(parent, arg, parent.getDepth() + 1, null);

        parent.addChild(newNode);
        size++;

        return newNode;
    }

    // ========================================
    // DIRECT EXECUTION — Unified Tree Traversal
    // ========================================

    /**
     * Counts the minimum number of required descendant nodes in the shortest
     * path from this node's children to an executable leaf. Used to reserve
     * tokens for trailing arguments after a limited greedy.
     */
    private int countMinRequiredDescendants(CommandNode<S, ?> node) {
        if (node.isLast()) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        for (var child : node.getChildren()) {
            int count = child.getNumberOfParametersToConsume() + countMinRequiredDescendants(child);
            if (count < min) {
                min = count;
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /**
     * prefix matching - Optimized for tab completion
     */
    private static boolean fastStartsWith(String str, String prefix) {
        final int prefixLen = prefix.length();
        if (str.length() < prefixLen) {
            return false;
        }

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

    private boolean literalMatchesInput(@NotNull CommandNode<S, ?> node, @Nullable String input) {
        return input != null && node.isLiteral() && node.getData().asCommand().hasName(input);
    }

    private boolean literalMatchesPrefix(@NotNull CommandNode<S, ?> node, @Nullable String prefix, boolean hasPrefix) {
        if (!node.isLiteral() || !hasPrefix || prefix == null) {
            return true;
        }
        Command<S> command = node.getData().asCommand();
        if (fastStartsWith(command.getName(), prefix)) {
            return true;
        }
        for (String alias : command.aliases()) {
            if (fastStartsWith(alias, prefix)) {
                return true;
            }
        }
        return false;
    }

    private void addSuggestions(
            List<String> results,
            @Nullable List<String> suggestions,
            @Nullable String prefix,
            boolean hasPrefix
    ) {
        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }
        if (!hasPrefix || prefix == null) {
            results.addAll(suggestions);
            return;
        }
        for (String suggestion : suggestions) {
            if (fastStartsWith(suggestion, prefix)) {
                results.add(suggestion);
            }
        }
    }

    private int getMatchDepthHeuristic(CommandNode<S, ?> node) {
        return node.getDepth();
    }

    /**
     * Directly traverses the tree and executes the matching pathway in one step.
     * <p>
     * The algorithm:
     * 1. Check root permission
     * 2. If no input, execute the root's default pathway
     * 3. Recursively traverse children matching each input token
     * 4. When a terminal executable node is found, create ExecutionContext, resolve args, and execute
     * </p>
     */
    @Override
    public @NotNull TreeExecutionResult<S> execute(
            @NotNull ExecutionContext<S> context,
            @NotNull ArgumentInput input
    ) throws CommandException {
        ensureNodeCaches();

        // Step 1: Check root permission
        Pair<PermissionHolder, Boolean> rootPermissionResult = permissionChecker.checkPermission(context.source(), root.getData());
        if (!rootPermissionResult.right()) {
            assert root.getExecutableUsage() != null;
            return TreeExecutionResult.permissionDenied(root.getExecutableUsage(), rootPermissionResult.left(), rootCommand);
        }

        // Step 2: Empty input — execute default pathway
        if (input.isEmpty()) {
            return executeEmptyInput(context, root, new ArrayList<>(2));
        }

        // Step 3: Traverse tree children looking for a match
        final var rootChildren = root.getChildren();
        if (rootChildren.isEmpty()) {
            return noMatchFromNode(root);
        }

        // Step 4: Try each root child — find the best match
        TreeExecutionResult<S> bestResult = null;
        int bestDepth = -1;

        ArrayList<ParseResult<S>> parsedArguments = new ArrayList<>(Math.min(4, input.size()));
        for (var child : rootChildren) {
            parsedArguments.clear();
            TreeExecutionResult<S> result = traverse(context, input, child, 0, parsedArguments);

            if (result.isSuccess()) {
                return result; // Immediate success — stop
            }

            // Track the deepest non-success for better error reporting
            if (result.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                return result; // Permission denied — stop immediately
            }

            // For NO_MATCH, track the one that got deepest
            if (bestResult == null || getMatchDepthHeuristic(child) > bestDepth) {
                bestResult = result;
                bestDepth = getMatchDepthHeuristic(child);
            }
        }

        return bestResult != null ? bestResult
                       : TreeExecutionResult.noMatch(rootCommand.getDefaultPathway(), rootCommand);
    }

    /**
     * Checks if the raw token at {@code depth} is a flag token.
     * If so, returns how many positions to skip (1 for switch, 2 for value flag).
     * Returns 0 if the token is not a flag.
     */
    private int computeFlagSkip(CommandContext<S> context, CommandNode<S, ?> node, int depth) {
        String rawToken = context.arguments().getOr(depth, null);
        if (rawToken == null || !Patterns.isInputFlag(rawToken)) {
            return 0;
        }
        // Find the matching flag from the nearest executable pathway
        FlagData<S> flagData = findFlagForToken(node, rawToken);
        if (flagData == null) {
            return 0; // Not a recognized flag — treat as normal input
        }
        return flagData.isSwitch() ? 1 : 2;
    }

    /**
     * Recursive tree traversal that finds the matching pathway and executes it directly.
     */
    private @NotNull TreeExecutionResult<S> traverse(
            ExecutionContext<S> context,
            ArgumentInput input,
            @NotNull CommandNode<S, ?> currentNode,
            int depth,
            @NotNull ArrayList<ParseResult<S>> parsedArguments
    ) throws CommandException {
        final int inputSize = input.size();

        // Guard: depth out of range
        if (depth >= inputSize) {
            return noMatchFromNode(currentNode);
        }

        // Skip flag tokens in the raw input — flags are not part of the tree structure,
        // they are handled during argument resolution by the Cursor/OptionalValueHandler.
        // Switch: skip 1 token (-s). Value flag: skip 2 tokens (-f value).
        int flagSkip = computeFlagSkip(context, currentNode, depth);
        if (flagSkip > 0) {
            return traverse(context, input, currentNode, depth + flagSkip, parsedArguments);
        }

        // Check permission BEFORE processing
        Pair<PermissionHolder, Boolean> currentNodePermissionResult = permissionChecker.checkPermission(context.source(), currentNode.getData());
        if (!currentNodePermissionResult.right()) {
            return TreeExecutionResult.permissionDenied(
                    Objects.requireNonNull(currentNode.isExecutable() ? currentNode.getExecutableUsage() : root.getData().getDefaultPathway()),
                    currentNodePermissionResult.left(),
                    getCommandFromNode(currentNode)
            );
        }

        // Greedy parameter handling
        if (currentNode.isGreedyParam()) {
            int greedyLimit = currentNode.getData().greedyLimit();
            CommandPathway<S> flagScopePathway = resolveFlagScopePathway(currentNode);

            // LIMITED greedy with children — reserve tokens for trailing args,
            // then try from max consumption down to 1 to find a valid child match.
            if (greedyLimit > 0 && !currentNode.isLast()) {
                int tokensAvailable = countRemainingBindableInput(context, currentNode, input, depth);
                int childrenNeeded = countMinRequiredDescendants(currentNode);
                int maxConsume = Math.min(greedyLimit, tokensAvailable - childrenNeeded);
                ParseResult<S> fallbackGreedyMatch = null;

                // Try from highest consumption downward (greedy preference)
                for (int consume = maxConsume; consume >= 1; consume--) {
                    ParseResult<S> greedyParseResult = currentNode.parse(depth, context, flagScopePathway, consume);
                    if (greedyParseResult.isFailure()) {
                        continue;
                    }

                    if (fallbackGreedyMatch == null) {
                        fallbackGreedyMatch = greedyParseResult;
                    }

                    int nextDepth = greedyParseResult.getNextDepth();
                    boolean appendedGreedy = appendParsedArgument(parsedArguments, currentNode, greedyParseResult);
                    try {
                        for (var child : currentNode.getChildren()) {
                            TreeExecutionResult<S> result = traverse(context, input, child, nextDepth, parsedArguments);
                            if (result.isSuccess() || result.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                                return result;
                            }
                        }
                    } finally {
                        rollbackParsedArgument(parsedArguments, appendedGreedy);
                    }
                }
                // If children didn't match but this node is executable, execute here
                if (currentNode.isExecutable() && fallbackGreedyMatch != null) {
                    assert currentNode.getExecutableUsage() != null;
                    boolean appendedFallback = appendParsedArgument(parsedArguments, currentNode, fallbackGreedyMatch);
                    try {
                        return executePathway(context, currentNode, currentNode.getExecutableUsage(), getCommandFromNode(currentNode),
                                parsedArguments);
                    } finally {
                        rollbackParsedArgument(parsedArguments, appendedFallback);
                    }
                }
                return noMatchFromNode(currentNode);
            }

            // UNLIMITED greedy — consumes all remaining input, short-circuit
            // let's match input
            ParseResult<S> greedyParseResult = currentNode.parse(depth, context, flagScopePathway);
            if (greedyParseResult.isFailure()) {
                return noMatchFromNode(currentNode);
            }
            boolean appendedGreedy = appendParsedArgument(parsedArguments, currentNode, greedyParseResult);
            try {
                return handleTerminalNode(context, currentNode, parsedArguments);
            } finally {
                rollbackParsedArgument(parsedArguments, appendedGreedy);
            }
        }

        // Check if current input matches this node
        final ParseResult<S> nodeParseResult;
        final int nextDepth;
        if (currentNode.isLiteral()) {
            if (!literalMatchesInput(currentNode, context.arguments().getOr(depth, null))) {
                return handleOptionalSkip(context, input, currentNode, depth, parsedArguments);
            }
            nodeParseResult = null;
            nextDepth = depth + 1;
        } else {
            nodeParseResult = currentNode.parse(depth, context, resolveFlagScopePathway(currentNode));
            if (nodeParseResult.isFailure()) {
                // If optional, try to skip this node and check children
                return handleOptionalSkip(context, input, currentNode, depth, parsedArguments);
            }
            nextDepth = nodeParseResult.getNextDepth();
        }

        boolean appendedCurrent = appendParsedArgument(parsedArguments, currentNode, nodeParseResult);
        try {
            // Node matches — determine if we're at the terminal position
            boolean isTerminal = !hasRemainingBindableInput(context, currentNode, input, nextDepth);

            if (isTerminal) {
                TreeExecutionResult<S> terminalResult = handleTerminalNode(context, currentNode, parsedArguments);
                if (terminalResult.isSuccess() || terminalResult.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                    return terminalResult;
                }
                // Terminal failed — if optional, try skipping (backtrack)
                if (currentNode.isOptional()) {
                    TreeExecutionResult<S> skipResult = tryOptionalBacktrack(
                            context,
                            input,
                            currentNode,
                            depth,
                            parsedArguments,
                            appendedCurrent,
                            nodeParseResult
                    );
                    if (skipResult.isSuccess() || skipResult.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                        return skipResult;
                    }
                }
                return terminalResult;
            }

            final var children = currentNode.getChildren();

            if (children.isEmpty()) {
                // No children — execute if no unconsumed non-flag input tokens left
                if (currentNode.isExecutable() && !hasRemainingBindableInput(context, currentNode, input, nextDepth)) {
                    assert currentNode.getExecutableUsage() != null;
                    return executePathway(context, currentNode, currentNode.getExecutableUsage(), getCommandFromNode(currentNode), parsedArguments);
                }
                return noMatchFromNode(currentNode);
            }

            // Try each child (consume path)
            TreeExecutionResult<S> bestChildResult = null;

            for (var child : children) {
                TreeExecutionResult<S> result = traverse(context, input, child, nextDepth, parsedArguments);
                if (result.isSuccess()) {
                    return result;
                }
                if (result.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                    return result;
                }
                if (bestChildResult == null) {
                    bestChildResult = result;
                }
            }

            // Children consume path failed — try backtracking: skip this optional
            // and try children at the SAME depth (don't consume the token)
            if (currentNode.isOptional()) {
                TreeExecutionResult<S> skipResult = tryOptionalBacktrack(
                        context,
                        input,
                        currentNode,
                        depth,
                        parsedArguments,
                        appendedCurrent,
                        nodeParseResult
                );
                if (skipResult.isSuccess() || skipResult.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                    return skipResult;
                }
            }

            // None of the children matched — if current node is executable, execute it
            // but only if the effective input (non-flag tokens) doesn't exceed the pathway's capacity
            if (currentNode.isExecutable()) {
                CommandPathway<S> pathway = currentNode.getExecutableUsage();
                assert pathway != null;
                if (!hasRemainingBindableInput(context, currentNode, input, nextDepth)) {
                    return executePathway(context, currentNode, pathway, getCommandFromNode(currentNode), parsedArguments);
                }
            }

            return bestChildResult != null ? bestChildResult : noMatchFromNode(currentNode);
        } finally {
            rollbackParsedArgument(parsedArguments, appendedCurrent);
        }
    }

    private TreeExecutionResult<S> tryOptionalBacktrack(
            ExecutionContext<S> context,
            ArgumentInput input,
            CommandNode<S, ?> currentNode,
            int depth,
            @NotNull ArrayList<ParseResult<S>> parsedArguments,
            boolean appendedCurrent,
            @Nullable ParseResult<S> nodeParseResult
    ) throws CommandException {
        if (!appendedCurrent) {
            return handleOptionalSkip(context, input, currentNode, depth, parsedArguments);
        }

        rollbackParsedArgument(parsedArguments, true);
        try {
            return handleOptionalSkip(context, input, currentNode, depth, parsedArguments);
        } finally {
            if (nodeParseResult != null) {
                parsedArguments.add(nodeParseResult);
            }
        }
    }

    /**
     * Computes the effective input size by subtracting flag tokens from the total.
     * This gives the number of "real" argument tokens that the tree needs to match.
     */
    private boolean hasRemainingBindableInput(
            CommandContext<S> context,
            CommandNode<S, ?> node,
            ArgumentInput input,
            int startFrom
    ) {
        for (int i = startFrom; i < input.size(); i++) {
            String token = context.arguments().getOr(i, null);
            if (token == null) {
                continue;
            }

            FlagData<S> flagData = findFlagForToken(node, token);
            if (flagData != null) {
                if (!flagData.isSwitch()) {
                    i++;
                }
                continue;
            }

            return true;
        }
        return false;
    }

    /**
     * Looks up a flag by raw token (e.g. "-s", "--silent") from the currently
     * scoped executable pathway only.
     */
    private @Nullable FlagData<S> findFlagForToken(CommandNode<S, ?> node, String rawToken) {
        FlagArgument<S> flag = findFlagArgumentForToken(node, rawToken);
        return flag == null ? null : flag.flagData();
    }

    private int countRemainingBindableInput(
            CommandContext<S> context,
            CommandNode<S, ?> node,
            ArgumentInput input,
            int startFrom
    ) {
        int count = 0;
        for (int i = startFrom; i < input.size(); i++) {
            String token = context.arguments().getOr(i, null);
            if (token == null) {
                continue;
            }

            FlagData<S> flagData = findFlagForToken(node, token);
            if (flagData != null) {
                if (!flagData.isSwitch()) {
                    i++;
                }
                continue;
            }

            count++;
        }
        return count;
    }

    private @Nullable FlagArgument<S> findFlagArgumentForToken(CommandNode<S, ?> node, String rawToken) {
        if (node == null) {
            return null;
        }

        String normalized = normalizeFlagToken(rawToken);
        if (normalized == null) {
            return null;
        }

        return node.getCompletionCache().flagLookup().get(normalized);
    }

    private @Nullable String normalizeFlagToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            return null;
        }

        String normalized = Patterns.isInputFlag(rawToken) ? Patterns.withoutFlagSign(rawToken) : rawToken;
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Handles the terminal node case: when the current depth matches the last input position.
     */
    private TreeExecutionResult<S> handleTerminalNode(
            ExecutionContext<S> context,
            CommandNode<S, ?> node,
            @NotNull List<ParseResult<S>> parsedArguments
    ) throws CommandException {
        if (node.isExecutable()) {
            assert node.getExecutableUsage() != null;
            return executePathway(context, node, node.getExecutableUsage(), getCommandFromNode(node), parsedArguments);
        }


        return noMatchFromNode(node);
    }

    private @NotNull TreeExecutionResult<S> executeEmptyInput(
            @NotNull ExecutionContext<S> context,
            @NotNull CommandNode<S, ?> currentNode,
            @NotNull ArrayList<ParseResult<S>> parsedArguments
    ) throws CommandException {
        if (!currentNode.isRoot()) {
            Pair<PermissionHolder, Boolean> permissionResult = permissionChecker.checkPermission(context.source(), currentNode.getData());
            if (!permissionResult.right()) {
                CommandPathway<S> deniedUsage = currentNode.getNearestExecutableUsage();
                return TreeExecutionResult.permissionDenied(deniedUsage, permissionResult.left(), getCommandFromNode(currentNode));
            }
        }

        CommandPathway<S> directPathway = currentNode.isRoot() ? getDirectExecutableUsage(currentNode) : currentNode.getExecutableUsage();
        if (directPathway != null) {
            return executePathway(context, currentNode, directPathway, getCommandFromNode(currentNode), parsedArguments);
        }

        TreeExecutionResult<S> bestNoMatch = null;
        for (var child : currentNode.getChildren()) {
            if (!child.isOptional()) {
                continue;
            }

            TreeExecutionResult<S> result = executeEmptyInput(context, child, parsedArguments);
            if (result.isSuccess() || result.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                return result;
            }
            if (bestNoMatch == null) {
                bestNoMatch = result;
            }
        }

        return bestNoMatch != null ? bestNoMatch : noMatchFromNode(currentNode);
    }

    /**
     * Handles optional parameter skipping: when the current node doesn't match,
     * try to skip it (if optional) and continue with its children.
     */
    private TreeExecutionResult<S> handleOptionalSkip(
            ExecutionContext<S> context,
            ArgumentInput input,
            CommandNode<S, ?> currentNode,
            int depth,
            @NotNull ArrayList<ParseResult<S>> parsedArguments
    ) throws CommandException {
        // If node is required, it's a hard fail — no match
        if (!currentNode.isOptional()) {
            return noMatchFromNode(currentNode);
        }

        // Optional node didn't match — if it's executable, we can execute (skipping the optional arg)
        if (currentNode.isExecutable()) {
            assert currentNode.getExecutableUsage() != null;
            return executePathway(context, currentNode, currentNode.getExecutableUsage(), getCommandFromNode(currentNode), parsedArguments);
        }

        // Try children at the SAME depth (we're skipping this optional node)
        final var children = currentNode.getChildren();
        for (var child : children) {
            if (!hasPermission(context.source(), child)) {
                continue;
            }
            TreeExecutionResult<S> result = traverse(context, input, child, depth, parsedArguments);
            if (result.isSuccess() || result.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                return result;
            }
        }

        return noMatchFromNode(currentNode);
    }

    /**
     * Creates a NO_MATCH result from a node, using the closest executable usage for error reporting.
     */
    private TreeExecutionResult<S> noMatchFromNode(CommandNode<S, ?> node) {
        CommandPathway<S> closest = node.getNearestExecutableUsage();
        return TreeExecutionResult.noMatch(closest, getCommandFromNode(node));
    }

    /**
     * Creates an ExecutionContext, resolves all arguments, and executes the pathway.
     * This is the core of the "direct execution" approach.
     * <p>
     * The pathway is guaranteed to be a complete merged pathway that includes
     * all literal/subcommand arguments in front of the actual parameters.
     * This means the Cursor will see a full parameter list that matches the raw input 1:1.
     */
    private TreeExecutionResult<S> executePathway(
            ExecutionContext<S> executionContext,
            @NotNull CommandNode<S, ?> currentNode,
            @NotNull CommandPathway<S> pathway,
            @NotNull Command<S> lastCommand,
            @NotNull List<ParseResult<S>> parsedArguments
    ) throws CommandException {
        ImperatDebugger.debug("Executing pathway: %s", pathway.formatted());

        // Create the execution context using the factory
        CommandPathway<S> closestUsage = currentNode.getNearestExecutableUsage();
        if (closestUsage == null) {
            closestUsage = lastCommand.getDefaultPathway();
        }
        executionContext.setDetectedPathway(pathway);
        return TreeExecutionResult.success(executionContext, closestUsage, pathway, lastCommand, parsedArguments);
    }

    private void refreshNodeCaches() {
        rootHasDedicatedDefaultPathway = hasDedicatedDefaultPathway();
        computeNearestExecutableUsage(root, getDirectExecutableUsage(root));
        computeCompletionCaches(root);
        nodeCachesDirty = false;
    }

    private void markNodeCachesDirty() {
        nodeCachesDirty = true;
    }

    private void ensureNodeCaches() {
        if (!nodeCachesDirty) {
            return;
        }
        refreshNodeCaches();
    }

    private @Nullable CommandPathway<S> computeNearestExecutableUsage(
            @NotNull CommandNode<S, ?> node,
            @Nullable CommandPathway<S> inheritedFallback
    ) {
        CommandPathway<S> directUsage = getDirectExecutableUsage(node);
        CommandPathway<S> bestUsage = directUsage != null ? directUsage : inheritedFallback;

        for (var child : node.getChildren()) {
            CommandPathway<S> childUsage = computeNearestExecutableUsage(child, bestUsage);
            if (bestUsage == null && childUsage != null) {
                bestUsage = childUsage;
            }
        }

        node.setNearestExecutableUsage(bestUsage);
        return bestUsage;
    }

    private boolean hasDedicatedDefaultPathway() {
        for (CommandPathway<S> pathway : rootCommand.getDedicatedPathways()) {
            if (pathway.isDefault()) {
                return true;
            }
        }
        return false;
    }

    private @Nullable CommandPathway<S> getDirectExecutableUsage(@NotNull CommandNode<S, ?> node) {
        CommandPathway<S> usage = node.getExecutableUsage();
        if (usage == null) {
            return null;
        }
        if (!node.isRoot()) {
            return usage;
        }
        return rootHasDedicatedDefaultPathway ? usage : null;
    }

    private void computeCompletionCaches(@NotNull CommandNode<S, ?> node) {
        List<FlagArgument<S>> visibleFlags = collectVisibleFlags(node);
        ChildCompletionCache<S> childCompletionCache = createChildCompletionCache(node);
        node.setCompletionCache(new CommandNode.CompletionCache<>(
                getResolverCached(node.getData()),
                visibleFlags,
                createFlagLookup(visibleFlags),
                collectOptionalOverlapDescendants(node),
                childCompletionCache.literalChildren(),
                childCompletionCache.nonLiteralChildren(),
                childCompletionCache.literalChildLookup()
        ));

        for (var child : node.getChildren()) {
            computeCompletionCaches(child);
        }
    }

    private @NotNull List<FlagArgument<S>> collectVisibleFlags(@NotNull CommandNode<S, ?> node) {
        CommandPathway<S> scopedPathway = resolveFlagScopePathway(node);
        if (scopedPathway == null) {
            return List.of();
        }

        Set<FlagArgument<S>> flags = scopedPathway.getFlagExtractor().getRegisteredFlags();
        if (flags.isEmpty()) {
            return List.of();
        }

        return List.copyOf(flags);
    }

    private @NotNull Map<String, FlagArgument<S>> createFlagLookup(@NotNull List<FlagArgument<S>> visibleFlags) {
        if (visibleFlags.isEmpty()) {
            return Map.of();
        }

        Map<String, FlagArgument<S>> lookup = new HashMap<>(visibleFlags.size() * 2);
        for (FlagArgument<S> flag : visibleFlags) {
            lookup.put(flag.flagData().name().toLowerCase(Locale.ROOT), flag);
            for (String alias : flag.flagData().aliases()) {
                lookup.put(alias.toLowerCase(Locale.ROOT), flag);
            }
        }
        return Map.copyOf(lookup);
    }

    private @NotNull ChildCompletionCache<S> createChildCompletionCache(@NotNull CommandNode<S, ?> node) {
        if (node.getChildren().isEmpty()) {
            return ChildCompletionCache.empty();
        }

        List<CommandNode<S, ?>> literalChildren = new ArrayList<>();
        List<CommandNode<S, ?>> nonLiteralChildren = new ArrayList<>();
        Map<String, List<CommandNode<S, ?>>> literalLookup = new HashMap<>();

        for (var child : node.getChildren()) {
            if (!child.isLiteral()) {
                nonLiteralChildren.add(child);
                continue;
            }

            literalChildren.add(child);
            registerLiteralLookupEntry(literalLookup, child.getData().asCommand().getName(), child);
            for (String alias : child.getData().asCommand().aliases()) {
                registerLiteralLookupEntry(literalLookup, alias, child);
            }
        }

        if (literalChildren.isEmpty() && nonLiteralChildren.isEmpty()) {
            return ChildCompletionCache.empty();
        }

        Map<String, List<CommandNode<S, ?>>> immutableLookup;
        if (literalLookup.isEmpty()) {
            immutableLookup = Map.of();
        } else {
            Map<String, List<CommandNode<S, ?>>> copy = new HashMap<>(literalLookup.size());
            for (var entry : literalLookup.entrySet()) {
                copy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            immutableLookup = Map.copyOf(copy);
        }

        return new ChildCompletionCache<>(
                literalChildren.isEmpty() ? List.of() : List.copyOf(literalChildren),
                nonLiteralChildren.isEmpty() ? List.of() : List.copyOf(nonLiteralChildren),
                immutableLookup
        );
    }

    private void registerLiteralLookupEntry(
            @NotNull Map<String, List<CommandNode<S, ?>>> literalLookup,
            @NotNull String literalName,
            @NotNull CommandNode<S, ?> child
    ) {
        String normalized = literalName.toLowerCase(Locale.ROOT);
        literalLookup.computeIfAbsent(normalized, ignored -> new ArrayList<>(1)).add(child);
    }

    private @NotNull List<CommandNode<S, ?>> collectOptionalOverlapDescendants(@NotNull CommandNode<S, ?> node) {
        if (node.getChildren().isEmpty()) {
            return List.of();
        }

        List<CommandNode<S, ?>> descendants = new ArrayList<>();
        Set<Type> seenTypes = new HashSet<>();
        seenTypes.add(node.getData().valueType());
        collectOptionalOverlapDescendants(node, seenTypes, descendants);
        return descendants.isEmpty() ? List.of() : List.copyOf(descendants);
    }

    private void collectOptionalOverlapDescendants(
            @NotNull CommandNode<S, ?> currentNode,
            @NotNull Set<Type> seenTypes,
            @NotNull List<CommandNode<S, ?>> descendants
    ) {
        for (var nextNode : currentNode.getChildren()) {
            Type nextType = nextNode.getData().valueType();
            if (!seenTypes.add(nextType)) {
                continue;
            }

            descendants.add(nextNode);
            if (nextNode.isOptional()) {
                collectOptionalOverlapDescendants(nextNode, seenTypes, descendants);
            }
            seenTypes.remove(nextType);
        }
    }

    /**
     * Extracts the Command from a node, defaulting to rootCommand.
     */
    private Command<S> getCommandFromNode(CommandNode<S, ?> node) {
        if (node.isLiteral()) {
            return node.getData().asCommand();
        }
        // Walk up to find the closest literal/command node
        CommandNode<S, ?> current = node.getParent();
        while (current != null) {
            if (current.isLiteral()) {
                return current.getData().asCommand();
            }
            current = current.getParent();
        }
        return rootCommand;
    }

    @Override
    public HelpEntryList<S> queryHelp(@NotNull HelpQuery<S> query) {
        ensureNodeCaches();
        final HelpEntryList<S> results = new HelpEntryList<>();

        if (query.getLimit() <= 0) {
            return HelpEntryList.empty();
        }

        collectHelpEntries(root, query, results);
        return results;
    }

    /**
     * Collects help entries in deep hierarchical mode - full tree traversal with structure
     */
    private void collectHelpEntries(
            CommandNode<S, ?> node,
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
        // Secret commands are hidden from help — skip the entire subtree
        if (node.isSecret()) {
            return;
        }

        // Add current node ONLY if it has executableUsage (truly executable) and passes filters
        if (node.isExecutable()) {
            CommandPathway<S> pathway = node.getExecutableUsage();
            assert pathway != null;

            if (!node.isRoot() || query.getRootUsagePredicate().test(pathway)) {
                if (passesFilters(pathway, query.getFilters())) {
                    results.add(helpEntryFactory.createEntry(pathway));
                }
            }
        }

        // Recursively process children (DFS traversal) - continues even through command nodes
        for (var child : node.getChildren()) {
            collectHelpEntries(child, query, results);
        }
    }

    /**
     * Applies all filters to a pathway.
     * Short-circuits on first failed filter for efficiency.
     */
    private boolean passesFilters(CommandPathway<S> pathway, Queue<HelpFilter<S>> filters) {
        for (HelpFilter<S> filter : filters) {
            if (!filter.filter(pathway)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull SuggestionContext<S> context) {
        if (!hasAutoCompletionPermission(context.source(), root)) {
            return Collections.emptyList();
        }

        ensureNodeCaches();
        String prefix = context.getArgToComplete().value();
        boolean hasPrefix = !prefix.isBlank();

        Map<Argument<S>, SuggestionProvider<S>> candidates = new LinkedHashMap<>();
        collectMatchingChildren(root, 0, context, candidates);

        List<String> list = new ArrayList<>();
        for (var candidate : candidates.entrySet()) {
            Argument<S> arg = candidate.getKey();
            if (!hasAutoCompletionPermission(context.source(), arg)) {
                continue;
            }
            SuggestionProvider<S> suggestionProvider = candidate.getValue();
            list.addAll(suggestionProvider.provide(context, arg));

        }
        return !hasPrefix ? list : list.stream()
                                   .filter((suggestion) -> suggestion.startsWith(prefix))
                                   .toList();
    }

    private void collectMatchingChildren(
            @NotNull CommandNode<S, ?> parentNode,
            int depth,
            @NotNull SuggestionContext<S> context,
            @NotNull Map<Argument<S>, SuggestionProvider<S>> candidates
    ) {
        if (parentNode.getChildren().isEmpty()) {
            return;
        }

        String token = context.arguments().getOr(depth, null);
        boolean hasConcreteToken = token != null && !token.isBlank();
        boolean flagPosition = hasConcreteToken && context.isFlagPosition(depth);

        if (hasConcreteToken && !flagPosition) {
            List<CommandNode<S, ?>> literalMatches = parentNode.getCompletionCache()
                                                             .literalChildLookup()
                                                             .get(token.toLowerCase(Locale.ROOT));
            if (literalMatches != null) {
                for (var childNode : literalMatches) {
                    findLongestMatchingNodes(depth, childNode, context, candidates);
                }
            }
        }

        for (var childNode : parentNode.getCompletionCache().nonLiteralChildren()) {
            findLongestMatchingNodes(depth, childNode, context, candidates);
        }

        if (!hasConcreteToken || flagPosition) {
            for (var childNode : parentNode.getCompletionCache().literalChildren()) {
                findLongestMatchingNodes(depth, childNode, context, candidates);
            }
        }
    }

    private @NotNull Map<Argument<S>, SuggestionProvider<S>> findLongestMatchingNodes(
            int depth,
            CommandNode<S, ?> currentNode,
            final SuggestionContext<S> context,
            Map<Argument<S>, SuggestionProvider<S>> candidates
    ) {
        if (!hasAutoCompletionPermission(context.source(), currentNode)) {
            return candidates;
        }

        int lastIndex = context.getArgToComplete().index();
        if (depth >= lastIndex) {

            final int prevIndex = lastIndex - 1;
            String prevInput = context.arguments().getOr(prevIndex, null);
            var prevNode = currentNode.getParent();

            if (prevInput != null && context.isFlagPosition(prevIndex) && prevNode != null) {
                // if its a true flag then we do not add the current noe
                FlagArgument<S> flagData = findFlagArgumentForToken(prevNode, prevInput);

                if (flagData != null && !flagData.flagData().isSwitch()) {
                    addSuggestionCandidate(flagData, candidates);
                    return candidates;
                }

            }

            addSuggestionCandidate(currentNode, candidates);
            if (prevNode != null) {
                var flagScope = resolveFlagScopePathway(prevNode);
                if (flagScope != null) {
                    List<FlagArgument<S>> unusedFlags = new ArrayList<>(prevNode.getCompletionCache().visibleFlags());
                    for (int i = lastIndex - 1; i >= 0; i--) {

                        String argInput = context.arguments().getOr(i, null);
                        if (argInput == null) {
                            break;
                        }
                        unusedFlags.removeIf((flagArg) -> flagArg.flagData().acceptsInput(argInput));
                    }
                    addSuggestionCandidates(unusedFlags, candidates);
                }
            }

            if (imperatConfig.isOptionalParameterSuggestionOverlappingEnabled() && currentNode.isOptional() && !currentNode.isLast()
                        && !currentNode.isTrueFlag()) {
                collectOverlappingNodes(currentNode, candidates);
            }

            return candidates;
        }

        var flagsScope = resolveFlagScopePathway(currentNode);
        if (nodeMatches(currentNode, depth, context, flagsScope)) {
            if (currentNode.isLast()) {
                //we are in a situation of extra input args , extra apart from the nodes!
                //TODO check for flags
                List<FlagArgument<S>> unusedFlags = currentNode.getCompletionCache().visibleFlags();
                for (int i = depth + 1; i < lastIndex - 1; i++) {
                    if (!context.isFlagPosition(i)) {
                        continue;
                    }
                    String argInput = context.arguments().getOr(i, null);
                    if (argInput == null) {
                        break;
                    }
                    unusedFlags.removeIf((flagArg) -> flagArg.flagData().acceptsInput(argInput));
                }
                addSuggestionCandidates(unusedFlags, candidates);

            } else {
                collectMatchingChildren(
                        currentNode,
                        depth + currentNode.getNumberOfParametersToConsume(),
                        context,
                        candidates
                );
            }
            return candidates;
        } else {
            if (context.isFlagPosition(depth)) {
                findLongestMatchingNodes(depth + 1, currentNode, context, candidates);
            }
        }

        return candidates;
    }

    private void collectOverlappingNodes(CommandNode<S, ?> curr, Map<Argument<S>, SuggestionProvider<S>> candidates) {
        for (var child : curr.getChildren()) {

            //check if parent and child are of same data-type, therefore impossible to differentiate between the two.

            addSuggestionCandidate(child, candidates);
            if (child.isRequired()) {
                break;
            }

            collectOverlappingNodes(child, candidates);


        }
    }

    private boolean nodeMatches(CommandNode<S, ?> node, int depth, SuggestionContext<S> ctx, CommandPathway<S> flagsScope) {
        String token = ctx.arguments().getOr(depth, null);
        if (token == null) {
            return false;
        }

        if (ctx.isFlagPosition(depth)) {
            return false;
        }

        if (node.isLiteral()) {
            return node.getData().asCommand().hasName(token);
        }

        if (isPlainStringCompletionNode(node)) {
            return true;
        }

        ParseResult<S> parseResult = node.parse(depth, ctx, flagsScope);
        return !parseResult.isFailure();
    }

    private boolean appendParsedArgument(
            @NotNull ArrayList<ParseResult<S>> parsedArguments,
            @NotNull CommandNode<S, ?> currentNode,
            @Nullable ParseResult<S> parseResult
    ) {
        if (parseResult == null || !parseResult.canReuseInExecution() || currentNode.isLiteral()) {
            return false;
        }
        parsedArguments.add(parseResult);
        return true;
    }

    private void rollbackParsedArgument(@NotNull ArrayList<ParseResult<S>> parsedArguments, boolean appended) {
        if (appended) {
            parsedArguments.remove(parsedArguments.size() - 1);
        }
    }

    private boolean hasPermission(S source, CommandNode<S, ?> node) {
        return permissionChecker.hasPermission(source, node.data);
    }

    private boolean hasAutoCompletionPermission(S src, CommandNode<S, ?> node) {

        if (node.isLiteral() && node.data.asCommand().isSecret()) {
            return false;
        }

        if (node.isLiteral() && node.getData().asCommand().isIgnoringACPerms()) {
            return true;
        }
        return hasPermission(src, node);
    }

    private boolean hasAutoCompletionPermission(S src, Argument<S> arg) {

        if (arg.isCommand() && arg.asCommand().isSecret()) {
            return false;
        }

        if (arg.isCommand() && arg.asCommand().isIgnoringACPerms()) {
            return true;
        }
        return permissionChecker.hasPermission(src, arg);
    }


    private SuggestionProvider<S> getResolverCached(Argument<S> param) {
        return imperatConfig.getParameterSuggestionResolver(param);
    }

    private boolean isPlainStringCompletionNode(@NotNull CommandNode<S, ?> node) {
        if (node.isGreedyParam()) {
            return false;
        }

        Argument<S> argument = node.getData();
        ArgumentType<S, ?> type = argument.type();
        return type.getNumberOfParametersToConsume(argument) == 1
                       && TypeUtility.matches(type.type(), String.class);
    }

    private void addSuggestionCandidates(
            @NotNull List<? extends Argument<S>> arguments,
            @NotNull Map<Argument<S>, SuggestionProvider<S>> candidates
    ) {
        for (Argument<S> argument : arguments) {
            addSuggestionCandidate(argument, candidates);
        }
    }

    private void addSuggestionCandidate(
            @NotNull CommandNode<S, ?> node,
            @NotNull Map<Argument<S>, SuggestionProvider<S>> candidates
    ) {
        candidates.putIfAbsent(node.getData(), node.getCompletionCache().suggestionProvider());
    }

    private void addSuggestionCandidate(
            @NotNull Argument<S> argument,
            @NotNull Map<Argument<S>, SuggestionProvider<S>> candidates
    ) {
        if (candidates.containsKey(argument)) {
            return;
        }

        SuggestionProvider<S> suggestionProvider = getResolverCached(argument);
        candidates.put(argument, suggestionProvider);
    }

    private @Nullable CommandPathway<S> resolveFlagScopePathway(CommandNode<S, ?> node) {
        return node == null ? null : node.getNearestExecutableUsage();
    }

    private record ChildCompletionCache<S extends CommandSource>(
            @NotNull List<CommandNode<S, ?>> literalChildren,
            @NotNull List<CommandNode<S, ?>> nonLiteralChildren,
            @NotNull Map<String, List<CommandNode<S, ?>>> literalChildLookup
    ) {

        private static <S extends CommandSource> @NotNull ChildCompletionCache<S> empty() {
            return new ChildCompletionCache<>(List.of(), List.of(), Map.of());
        }
    }

}




















