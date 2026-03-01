package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.command.tree.help.HelpEntryFactory;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpFilter;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * N-ary tree implementation focused on maximum performance
 * @author Mqzen
 */
final class StandardCommandTree<S extends Source> implements CommandTree<S> {

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
    int size;

    StandardCommandTree(ImperatConfig<S> imperatConfig, Command<S> command) {
        this.rootCommand = command;
        this.root = CommandNode.createCommandNode(null, command, -1, command.getDefaultPathway());
        this.imperatConfig = imperatConfig;
        this.permissionChecker = imperatConfig.getPermissionChecker();
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
                              .parameters(mergedArgs)
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
            Context<S> context,
            @NotNull ArgumentInput input
    ) throws CommandException {

        // Step 1: Check root permission
        if (!hasPermission(context.source(), root)) {
            return TreeExecutionResult.permissionDenied(root.getExecutableUsage(), rootCommand);
        }

        // Step 2: Empty input — execute default pathway
        if (input.isEmpty()) {
            CommandPathway<S> defaultPathway = root.getExecutableUsage();
            if (defaultPathway == null) {
                return TreeExecutionResult.noMatch(rootCommand.getDefaultPathway(), rootCommand);
            }
            return executePathway(context, defaultPathway, rootCommand);
        }

        // Step 3: Traverse tree children looking for a match
        final var rootChildren = root.getChildren();
        if (rootChildren.isEmpty()) {
            // No children, but root has a default pathway — execute it
            CommandPathway<S> defaultPathway = root.getExecutableUsage();
            if (defaultPathway != null) {
                return executePathway(context, defaultPathway, rootCommand);
            }
            return TreeExecutionResult.noMatch(rootCommand.getDefaultPathway(), rootCommand);
        }

        // Step 4: Try each root child — find the best match
        TreeExecutionResult<S> bestResult = null;
        int bestDepth = -1;

        for (var child : rootChildren) {
            TreeExecutionResult<S> result = traverseAndExecute(context, input, child, 0);

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

    private int getMatchDepthHeuristic(CommandNode<S, ?> node) {
        return node.getDepth();
    }

    /**
     * Recursive tree traversal that finds the matching pathway and executes it directly.
     */
    private @NotNull TreeExecutionResult<S> traverseAndExecute(
            Context<S> context,
            ArgumentInput input,
            @NotNull CommandNode<S, ?> currentNode,
            int depth
    ) throws CommandException {
        final int inputSize = input.size();
        final int consumeCount = currentNode.getNumberOfParametersToConsume();

        // Guard: depth out of range
        if (depth >= inputSize) {
            return noMatchFromNode(currentNode);
        }

        // Skip flag tokens in the raw input — flags are not part of the tree structure,
        // they are handled during argument resolution by the Cursor/ParameterValueAssigner.
        // Switch: skip 1 token (-s). Value flag: skip 2 tokens (-f value).
        int flagSkip = computeFlagSkip(context, currentNode, depth);
        if (flagSkip > 0) {
            return traverseAndExecute(context, input, currentNode, depth + flagSkip);
        }

        // Check permission BEFORE processing
        if (!hasPermission(context.source(), currentNode)) {
            return TreeExecutionResult.permissionDenied(
                    currentNode.isExecutable() ? currentNode.getExecutableUsage() : null,
                    getCommandFromNode(currentNode)
            );
        }

        // Greedy parameter — consumes all remaining input
        if (currentNode.isGreedyParam()) {
            if (currentNode.isExecutable()) {
                return executePathway(context, currentNode.getExecutableUsage(), getCommandFromNode(currentNode));
            }
            return noMatchFromNode(currentNode);
        }

        // Check if current input matches this node
        boolean nodeMatches = currentNode.matchesInput(depth, context, currentNode.isOptional());

        if (!nodeMatches) {
            // If optional, try to skip this node and check children
            return handleOptionalSkip(context, input, currentNode, depth);
        }

        // Node matches — determine if we're at the terminal position
        // Account for remaining flag tokens after this node
        int effectiveInputSize = computeEffectiveInputSize(context, input, depth + consumeCount);
        boolean isTerminal = (depth == effectiveInputSize - consumeCount);

        if (isTerminal) {
            TreeExecutionResult<S> terminalResult = handleTerminalNode(context, input, currentNode, depth);
            if (terminalResult.isSuccess() || terminalResult.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                return terminalResult;
            }
            // Terminal failed — if optional, try skipping (backtrack)
            if (currentNode.isOptional()) {
                TreeExecutionResult<S> skipResult = handleOptionalSkip(context, input, currentNode, depth);
                if (skipResult.isSuccess() || skipResult.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                    return skipResult;
                }
            }
            return terminalResult;
        }

        // Not terminal — continue traversal to children
        int nextDepth = depth + consumeCount;
        final var children = currentNode.getChildren();

        if (children.isEmpty()) {
            // No children — execute if no unconsumed non-flag input tokens left
            if (currentNode.isExecutable() && nextDepth >= effectiveInputSize) {
                return executePathway(context, currentNode.getExecutableUsage(), getCommandFromNode(currentNode));
            }
            return noMatchFromNode(currentNode);
        }

        // Try each child (consume path)
        TreeExecutionResult<S> bestChildResult = null;

        for (var child : children) {
            TreeExecutionResult<S> result = traverseAndExecute(context, input, child, nextDepth);
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
            TreeExecutionResult<S> skipResult = handleOptionalSkip(context, input, currentNode, depth);
            if (skipResult.isSuccess() || skipResult.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                return skipResult;
            }
        }

        // None of the children matched — if current node is executable, execute it
        // but only if the effective input (non-flag tokens) doesn't exceed the pathway's capacity
        if (currentNode.isExecutable()) {
            CommandPathway<S> pathway = currentNode.getExecutableUsage();
            if (effectiveInputSize <= pathway.getArguments().size()) {
                return executePathway(context, pathway, getCommandFromNode(currentNode));
            }
        }

        return bestChildResult != null ? bestChildResult : noMatchFromNode(currentNode);
    }

    /**
     * Checks if the raw token at {@code depth} is a flag token.
     * If so, returns how many positions to skip (1 for switch, 2 for value flag).
     * Returns 0 if the token is not a flag.
     */
    private int computeFlagSkip(Context<S> context, CommandNode<S, ?> node, int depth) {
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
     * Computes the effective input size by subtracting flag tokens from the total.
     * This gives the number of "real" argument tokens that the tree needs to match.
     */
    private int computeEffectiveInputSize(Context<S> context, ArgumentInput input, int startFrom) {
        int flagTokens = 0;
        for (int i = startFrom; i < input.size(); i++) {
            String token = context.arguments().getOr(i, null);
            if (token != null && Patterns.isInputFlag(token)) {
                flagTokens++; // The flag name itself
                // Check if it's a value flag (has a value token after it)
                // We peek ahead to see if next token is NOT another flag
                if (i + 1 < input.size()) {
                    String next = context.arguments().getOr(i + 1, null);
                    if (next != null && !Patterns.isInputFlag(next)) {
                        flagTokens++; // The flag's value
                        i++; // Skip the value token too
                    }
                }
            }
        }
        return input.size() - flagTokens;
    }

    /**
     * Looks up a flag by raw token (e.g. "-s", "--silent") from the nearest
     * executable pathway reachable from the given node (self or ancestors).
     */
    private @Nullable FlagData<S> findFlagForToken(CommandNode<S, ?> node, String rawToken) {
        // Walk up to find the nearest executable pathway with registered flags
        CommandNode<S, ?> current = node;
        while (current != null) {
            if (current.isExecutable() && current.getExecutableUsage() != null) {
                FlagData<S> flag = matchFlagFromPathway(current.getExecutableUsage(), rawToken);
                if (flag != null) {
                    return flag;
                }
            }
            current = current.getParent();
        }
        // Also check root command's default pathway
        CommandPathway<S> defaultPathway = rootCommand.getDefaultPathway();
        FlagData<S> flag = matchFlagFromPathway(defaultPathway, rawToken);
        if (flag != null) {
            return flag;
        }
        return null;
    }

    /**
     * Checks if a raw token matches any registered flag in the given pathway.
     */
    private @Nullable FlagData<S> matchFlagFromPathway(CommandPathway<S> pathway, String rawToken) {
        if (pathway == null) {
            return null;
        }
        for (FlagArgument<S> flagArg : pathway.getFlagExtractor().getRegisteredFlags()) {
            if (flagArg.flagData().acceptsInput(rawToken)) {
                return flagArg.flagData();
            }
        }
        return null;
    }

    /**
     * Handles the terminal node case: when the current depth matches the last input position.
     */
    private TreeExecutionResult<S> handleTerminalNode(
            Context<S> context,
            ArgumentInput input,
            CommandNode<S, ?> node,
            int depth
    ) throws CommandException {
        if (!node.matchesInput(depth, context)) {
            return noMatchFromNode(node);
        }

        if (!hasPermission(context.source(), node)) {
            return TreeExecutionResult.permissionDenied(
                    node.isExecutable() ? node.getExecutableUsage() : null,
                    getCommandFromNode(node)
            );
        }

        if (node.isExecutable()) {
            return executePathway(context, node.getExecutableUsage(), getCommandFromNode(node));
        }

        // Literal node without executable usage — it's a valid match but no execution target
        if (node.isLiteral()) {
            // Try to find executable in children (e.g., default sub-pathway)
            return noMatchFromNode(node);
        }

        return noMatchFromNode(node);
    }

    /**
     * Handles optional parameter skipping: when the current node doesn't match,
     * try to skip it (if optional) and continue with its children.
     */
    private TreeExecutionResult<S> handleOptionalSkip(
            Context<S> context,
            ArgumentInput input,
            CommandNode<S, ?> currentNode,
            int depth
    ) throws CommandException {
        // If node is required, it's a hard fail — no match
        if (!currentNode.isOptional()) {
            return noMatchFromNode(currentNode);
        }

        // Optional node didn't match — if it's executable, we can execute (skipping the optional arg)
        if (currentNode.isExecutable()) {
            return executePathway(context, currentNode.getExecutableUsage(), getCommandFromNode(currentNode));
        }

        // Try children at the SAME depth (we're skipping this optional node)
        final var children = currentNode.getChildren();
        for (var child : children) {
            if (!hasPermission(context.source(), child)) {
                continue;
            }
            TreeExecutionResult<S> result = traverseAndExecute(context, input, child, depth);
            if (result.isSuccess() || result.getStatus() == TreeExecutionResult.Status.PERMISSION_DENIED) {
                return result;
            }
        }

        return noMatchFromNode(currentNode);
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
            Context<S> context,
            @NotNull CommandPathway<S> pathway,
            @NotNull Command<S> lastCommand
    ) throws CommandException {
        ImperatDebugger.debug("Executing pathway: %s", pathway.formatted());

        // Create the execution context using the factory
        ExecutionContext<S> executionContext = imperatConfig.getContextFactory()
                                                       .createExecutionContext(context, pathway, lastCommand);

        // Resolve all arguments using the ParameterValueAssigner chain
        executionContext.resolve();

        return TreeExecutionResult.success(executionContext, pathway, lastCommand);
    }

    /**
     * Creates a NO_MATCH result from a node, using the closest executable usage for error reporting.
     */
    private TreeExecutionResult<S> noMatchFromNode(CommandNode<S, ?> node) {
        CommandPathway<S> closest = findClosestUsage(node);
        return TreeExecutionResult.noMatch(closest, getCommandFromNode(node));
    }

    /**
     * Finds the closest executable usage starting from a node,
     * searching upwards to parents and then down to children.
     */
    private @Nullable CommandPathway<S> findClosestUsage(CommandNode<S, ?> node) {
        // First check the node itself
        if (node.isExecutable()) {
            return node.getExecutableUsage();
        }

        // Check parent chain
        CommandNode<S, ?> parent = node.getParent();
        while (parent != null) {
            if (parent.isExecutable()) {
                return parent.getExecutableUsage();
            }
            parent = parent.getParent();
        }

        // Check children (BFS)
        for (var child : node.getChildren()) {
            if (child.isExecutable()) {
                return child.getExecutableUsage();
            }
        }

        return rootCommand.getDefaultPathway();
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
        // Apply filters to current node
        if (!passesFilters(node, query.getFilters())) {
            return;
        }

        // Add current node ONLY if it has executableUsage (truly executable)
        if (node.isExecutable()) {
            if (!node.isRoot() || /*Root Node :D*/ query.getRootUsagePredicate().test(node.getExecutableUsage())) {
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
    private boolean passesFilters(CommandNode<S, ?> node, Queue<HelpFilter<S>> filters) {
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

        if (!hasAutoCompletionPermission(context.source(), root)) {
            return Collections.emptyList();
        }

        for (var child : root.getChildren()) {
            tabCompleteNode(child, context, 0, results);
        }
        return results.stream()
                       .filter((suggestion) -> !hasPrefix || fastStartsWith(suggestion, prefix))
                       .toList();
    }

    private void tabCompleteNode(
            final CommandNode<S, ?> node,
            final SuggestionContext<S> context,
            int inputDepth,
            final List<String> results
    ) {

        int lastIndex = context.getArgToComplete().index();
        if (inputDepth > lastIndex) {
            return;
        }

        if (inputDepth == lastIndex) {
            // Check if the previous token was a flag — if so, suggest flag values
            if (inputDepth > 0) {
                String prevToken = context.arguments().getOr(inputDepth - 1, null);
                if (prevToken != null && Patterns.isInputFlag(prevToken)) {
                    FlagData<S> flagData = findFlagForToken(node, prevToken);
                    if (flagData != null && !flagData.isSwitch()) {
                        // We're completing the value for this flag
                        collectFlagValueSuggestions(node, flagData, context, results);
                        return;
                    }
                }
            }

            results.addAll(getResolverCached(node.data).provide(context, node.data));
            // Also suggest flags at this position
            collectFlagNameSuggestions(node, context, results);

            if (imperatConfig.isOptionalParameterSuggestionOverlappingEnabled() && node.isOptional() && !(node.isTrueFlag())) {
                collectOverlappingSuggestions(node, node, context, results);
            }
        } else {
            String currentInput = context.arguments().getOr(inputDepth, null);
            assert currentInput != null;
            if (!hasAutoCompletionPermission(context.source(), node)) {
                return;
            }

            if (node.isGreedyParam()) {
                tabCompleteNode(node, context, lastIndex, results);
                return;
            }

            // Skip flag tokens in the input
            if (Patterns.isInputFlag(currentInput)) {
                FlagData<S> flagData = findFlagForToken(node, currentInput);
                if (flagData != null) {
                    if (!flagData.isSwitch() && inputDepth + 1 == lastIndex) {
                        // The user is completing the flag's VALUE — provide value suggestions
                        collectFlagValueSuggestions(node, flagData, context, results);
                        return;
                    }
                    int skip = flagData.isSwitch() ? 1 : 2;
                    // Continue with the same node at the skipped depth
                    tabCompleteNode(node, context, inputDepth + skip, results);
                    return;
                }
            }

            if (!node.matchesInput(inputDepth, context, node.isOptional()) && node.isRequired()) {
                return;
            }

            for (var child : node.getChildren()) {
                tabCompleteNode(child, context, inputDepth + node.getNumberOfParametersToConsume(), results);
            }

        }
    }

    /**
     * Collects flag name suggestions (e.g., "-s", "--silent") from all reachable executable pathways.
     */
    private void collectFlagNameSuggestions(CommandNode<S, ?> node, SuggestionContext<S> context, List<String> results) {
        Set<FlagArgument<S>> flags = collectReachableFlags(node);
        for (FlagArgument<S> flag : flags) {
            results.add("-" + flag.flagData().name());
            for (String alias : flag.flagData().aliases()) {
                results.add("-" + alias);
            }
        }
    }

    /**
     * Collects flag value suggestions for a specific flag's input type.
     */
    private void collectFlagValueSuggestions(CommandNode<S, ?> node, FlagData<S> flagData, SuggestionContext<S> context, List<String> results) {
        // Find the FlagArgument that matches this FlagData
        Set<FlagArgument<S>> flags = collectReachableFlags(node);
        for (FlagArgument<S> flag : flags) {
            if (flag.flagData().name().equalsIgnoreCase(flagData.name())) {
                // Try the flag's specific suggestion resolver first
                SuggestionProvider<S> resolver = flag.inputSuggestionResolver();
                if (resolver != null) {
                    results.addAll(resolver.provide(context, flag));
                    return;
                }
                // Fall back to the input type's suggestion provider
                var inputType = flag.flagData().inputType();
                if (inputType != null) {
                    results.addAll(inputType.getSuggestionProvider().provide(context, flag));
                }
                return;
            }
        }
    }

    /**
     * Collects all registered flags from executable pathways reachable from
     * the given node (self and ancestors).
     */
    private Set<FlagArgument<S>> collectReachableFlags(CommandNode<S, ?> node) {
        CommandNode<S, ?> current = node;
        while (current != null) {
            if (current.isExecutable() && current.getExecutableUsage() != null) {
                var flags = current.getExecutableUsage().getFlagExtractor().getRegisteredFlags();
                if (!flags.isEmpty()) {
                    return flags;
                }
            }
            current = current.getParent();
        }
        // Also check root command's default pathway
        CommandPathway<S> defaultPathway = rootCommand.getDefaultPathway();
        if (defaultPathway != null) {
            var flags = defaultPathway.getFlagExtractor().getRegisteredFlags();
            if (!flags.isEmpty()) {
                return flags;
            }
        }
        return Set.of();
    }

    /**
     * Recursively collect suggestions from subsequent parameters with different types
     * Stops at required parameters (inclusive)
     */
    private void collectOverlappingSuggestions(
            CommandNode<S, ?> origin,
            CommandNode<S, ?> currentNode,
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
            if (!(nextNode.isLiteral() && nextNode.data.asCommand().isIgnoringACPerms())
                        && !hasPermission(context.source(), nextNode)) {
                //System.out.println("Skipping " + nextNode.format() + " due to having no perm for it");
                continue;
            }

            // Collect suggestions from this node
            //System.out.println("Getting from node " + nextNode.format() + "'s overlap:");
            final var resolver = getResolverCached(nextNode.data);
            final var suggestions = resolver.provide(context, nextNode.data);
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

    private boolean hasPermission(S source, CommandNode<S, ?> node) {
        return permissionChecker.hasPermission(source, node.data);
    }

    private boolean hasAutoCompletionPermission(S src, CommandNode<S, ?> node) {
        if (node.isLiteral() && node.getData().asCommand().isIgnoringACPerms()) {
            return true;
        }
        return hasPermission(src, node);
    }

    /**
     * CACHED resolver lookup - Eliminates config lookups
     */
    private SuggestionProvider<S> getResolverCached(Argument<S> param) {
        return imperatConfig.getParameterSuggestionResolver(param);
    }

    /**
     * Searches for closest usage to a context entered.
     * returns an ORDERED/SORTED set of unique {@link CommandPathway} , ordered by
     * how close they are to the context entered, the closest usage to the context entered shall be
     * placed on top of this ordered set of closest usages.
     *
     * @param context the context to search with.
     * @return the closest usages ordered by how close they are to a {@link Context}
     */
    @Override
    public Set<CommandPathway<S>> getClosestUsages(Context<S> context) {
        final var queue = context.arguments();
        final String firstArg = queue.getOr(0, null);

        final var startingNode = (firstArg == null) ? root : findStartingNode(context, root);

        return (startingNode == null)
                       ? Set.of(rootCommand.getDefaultPathway())
                       : getClosestUsagesRecursively(new LinkedHashSet<>(), startingNode, context);
    }

    private CommandNode<S, ?> findStartingNode(Context<S> context, CommandNode<S, ?> root) {
        for (var child : root.getChildren()) {
            if (child.matchesInput(child.getDepth(), context)) {
                return child;
            }
        }
        return null;
    }

    private Set<CommandPathway<S>> getClosestUsagesRecursively(
            Set<CommandPathway<S>> currentUsages,
            CommandNode<S, ?> node,
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
            Set<CommandPathway<S>> currentUsages,
            CommandNode<S, ?> child,
            Context<S> context
    ) {
        final var childUsages = getClosestUsagesRecursively(new LinkedHashSet<>(), child, context);
        currentUsages.addAll(childUsages);
    }

}




















