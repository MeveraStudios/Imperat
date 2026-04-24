package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.Patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * N-ary tree representation of a {@link Command} used for parsing and dispatching
 * user input to the correct executable pathway.
 *
 * <p>Each {@link Node} carries a required main {@link Argument} plus optional
 * arguments and flags inherited from its {@link CommandPathway}. Multiple
 * pathways that share a prefix converge onto the same nodes.</p>
 *
 * <p>Traversal is a DFS with input-stream backtracking: every node that accepts
 * the current input spawns a branch; UNACCEPTABLE results prune a branch. Among
 * all surviving branches the tree returns the one with the highest score (see
 * {@link #scoreCandidate}).</p>
 *
 * @param <S> the command source type
 */
public final class SuperCommandTree<S extends CommandSource> implements CommandTree<S> {

    static final int SUCCESSFUL_PARSE_SCORE = 1;
    static final int FAILED_PARSE_SCORE = 0;
    static final int UNACCEPTABLE_SCORE = -1;

    final Node<S> root;
    private final ImperatConfig<S> imperatConfig;
    private int size;

    public SuperCommandTree(ImperatConfig<S> imperatConfig, Command<S> command) {
        this.imperatConfig = imperatConfig;
        this.root = new Node<>(null, command.getDefaultPathway(), command);
        this.size = 1;
    }

    private static <S extends CommandSource> int subTreeSize(CommandTree<S> tree) {
        if (tree instanceof SuperCommandTree<S> sct) {
            return sct.size;
        }
        return countNodes(tree.rootNode());
    }

    private static <S extends CommandSource> int countNodes(Node<S> node) {
        int count = 1;
        for (Node<S> child : node.children.values()) {
            count += countNodes(child);
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Tree construction
    // ------------------------------------------------------------------

    @Override
    public @NotNull Node<S> rootNode() {
        return root;
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * Walks the pathway's PERSONAL arguments (excluding inherited flags), creating
     * or reusing child nodes keyed by argument name. Optional arguments are
     * accumulated onto the preceding required node; trailing optionals attach to
     * the last required node reached.
     *
     * <p>Flags are owned by the {@link CommandPathway#getFlagExtractor()} and are
     * not inserted as tree nodes; they are attached via the pathway reference
     * held by each node.</p>
     */
    @Override
    public void parseUsage(@NotNull CommandPathway<S> usage) {
        Node<S> current = root;
        List<Argument<S>> pendingOptionals = new ArrayList<>();

        for (Argument<S> arg : usage.getArguments()) {
            if (arg.isFlag()) {
                continue; // flags belong to the pathway's FlagExtractor
            }
            if (arg.isOptional()) {
                pendingOptionals.add(arg);
                continue;
            }
            // required argument: flush pending optionals onto current, then descend
            attachOptionals(current, pendingOptionals);
            pendingOptionals.clear();

            Node<S> child = current.children.get(arg.getName());
            if (child == null) {
                child = new Node<>(current, usage, arg);
                current.children.put(arg.getName(), child);
                size++;
            }
            current = child;
        }
        // trailing optionals (no more required args following)
        attachOptionals(current, pendingOptionals);
    }

    private void attachOptionals(Node<S> node, List<Argument<S>> optionals) {
        if (optionals.isEmpty()) {
            return;
        }
        for (Argument<S> opt : optionals) {
            boolean exists = node.optionals.stream()
                                     .anyMatch(o -> o.getName().equals(opt.getName()));
            if (!exists) {
                node.optionals.add(opt);
            }
        }
    }

    /**
     * Attaches the root of {@code subTree} as a child of the node inside this
     * tree whose {@code main.format()} equals {@code attachmentNode}.
     *
     * <p>Structural attach only: no merging of optionals/flags is performed. The
     * subtree's pathway and flags remain intact inside its own nodes.</p>
     */
    @Override
    public void parseSubTree(@NotNull CommandTree<S> subTree, String attachmentNode) {
        Node<S> target;
        if (attachmentNode == null || attachmentNode.isBlank()) {
            target = root;
        } else {
            target = findByFormat(root, attachmentNode);
        }
        if (target == null) {
            throw new IllegalArgumentException(
                    "No node with format '" + attachmentNode + "' in tree for command '"
                            + root.main.format() + "'");
        }
        Node<S> subRoot = subTree.rootNode();
        target.children.put(subRoot.main.getName(), subRoot);
        subRoot.parent = target;
        size += subTreeSize(subTree);
    }

    private @Nullable Node<S> findByFormat(Node<S> node, String format) {
        if (node.main.format().equals(format)
                    || node.optionals.stream().anyMatch(optional -> optional.format().equals(format))) {
            return node;
        }
        for (Node<S> child : node.children.values()) {
            Node<S> found = findByFormat(child, format);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Execution / traversal
    // ------------------------------------------------------------------

    /**
     * Traverses the tree against {@code input}, collecting every root-to-terminal
     * branch that did NOT fail with an UNACCEPTABLE score, then returns the
     * best-matching branch.
     *
     * <p>Terminal branches are: (a) a leaf node that parsed successfully, or
     * (b) any node that fully consumed the input.</p>
     *
     * <p>Branches are ranked by {@link #scoreCandidate} which prefers, in order:
     * full input consumption, higher per-argument score sum, greater depth,
     * fewer failed-parse arguments.</p>
     *
     * @return the best matching chain of {@link ParsedNode}s from root to the
     *         terminal node; an empty list if nothing matched.
     */
    @Override
    public @NotNull List<ParsedNode<S>> execute(ExecutionContext<S> context, @NotNull ArgumentInput input)
            throws CommandException {
        RawInputStream<S> stream = RawInputStream.newStream(context, input);
        int totalTokens = stream.size();

        List<Candidate<S>> candidates = new ArrayList<>();
        List<ParsedNode<S>> path = new ArrayList<>();
        traverse(root, stream, path, candidates);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Candidate<S> best = candidates.stream()
                                    .filter(c -> !hasUnacceptable(c.chain))
                                    .max(Comparator.comparingInt(c -> scoreCandidate(c, totalTokens)))
                                    .orElse(null);

        return best == null ? Collections.emptyList() : best.chain;
    }

    private void traverse(
            Node<S> node,
            RawInputStream<S> stream,
            List<ParsedNode<S>> path,
            List<Candidate<S>> candidates
    ) throws CommandException {

        if (node.isRoot()) {
            // root represents the command literal itself; it is NOT parsed from input
            // (the framework strips the label before dispatching). Insert a synthetic
            // parsed node so downstream consumers have the command on the chain.
            ParsedNode<S> rootParsed = new ParsedNode<>(node, new HashMap<>());
            path.add(rootParsed);

            if (node.isLeaf()) {
                if (!stream.hasNext()) {
                    candidates.add(new Candidate<>(new ArrayList<>(path), stream.getRawIndex()));
                }
            } else {
                if (!stream.hasNext()) {
                    // default pathway (no args) on a command that also has subcommands
                    candidates.add(new Candidate<>(new ArrayList<>(path), stream.getRawIndex()));
                }
                for (Node<S> child : node.children.values()) {
                    int saved = stream.getRawIndex();
                    traverse(child, stream, path, candidates);
                    stream.setRawIndex(saved);
                }
            }
            path.remove(path.size() - 1);
            return;
        }

        int saved = stream.getRawIndex();
        ParsedNode<S> parsed;
        try {
            parsed = node.parseArgument(stream);
        } catch (Throwable t) {
            // parse threw: treat as dead branch, backtrack
            stream.setRawIndex(saved);
            return;
        }
        if (parsed == null) {
            // UNACCEPTABLE main parse: prune branch
            stream.setRawIndex(saved);
            return;
        }

        path.add(parsed);
        int afterParse = stream.getRawIndex();

        if (node.isLeaf()) {
            candidates.add(new Candidate<>(new ArrayList<>(path), afterParse));
        } else if (!stream.hasNext()) {
            // input exhausted mid-tree: still a candidate (lower-scoring, but may be the best)
            candidates.add(new Candidate<>(new ArrayList<>(path), afterParse));
        } else {
            for (Node<S> child : node.children.values()) {
                int childSaved = stream.getRawIndex();
                traverse(child, stream, path, candidates);
                stream.setRawIndex(childSaved);
            }
        }

        path.remove(path.size() - 1);
        stream.setRawIndex(saved);
    }

    private boolean hasUnacceptable(List<ParsedNode<S>> chain) {
        for (ParsedNode<S> pn : chain) {
            for (ParseResult<S> r : pn.getParseResults().values()) {
                if (r.isUnAcceptableScore()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Weighted score. Priorities (highest tier first):
     * <ol>
     *   <li>Full input consumption (fullyConsumed bit)</li>
     *   <li>Sum of per-argument parse scores</li>
     *   <li>Depth of the chain</li>
     *   <li>Penalty for failed-parse arguments</li>
     * </ol>
     */
    private int scoreCandidate(Candidate<S> candidate, int totalTokens) {
        int consumed = candidate.consumedIndex + 1; // raw index is -1 at start
        boolean fullyConsumed = totalTokens == 0 || consumed >= totalTokens;

        int totalScore = 0;
        int failures = 0;
        int depth = candidate.chain.size();
        for (ParsedNode<S> pn : candidate.chain) {
            totalScore += pn.getTotalParseScore();
            for (ParseResult<S> r : pn.getParseResults().values()) {
                if (r.isFailureScore()) {
                    failures++;
                }
            }
        }

        return (fullyConsumed ? 1_000_000 : 0)
                       + totalScore * 1_000
                       + depth * 10
                       - failures;
    }

    // ------------------------------------------------------------------
    // Deferred: tab-complete / help
    // ------------------------------------------------------------------

    @Override
    public @NotNull List<String> tabComplete(@NotNull SuggestionContext<S> context) {
        ArgumentInput fullInput = context.arguments();
        int completionIndex = Math.max(0, context.getArgToComplete().index());

        if (hasBlankGap(fullInput, completionIndex)) {
            return Collections.emptyList();
        }

        String prefix = normalizePrefix(context.getArgToComplete().value());
        ArgumentInput consumedInput = ArgumentInput.empty();
        for (int i = 0; i < completionIndex && i < fullInput.size(); i++) {
            consumedInput.add(fullInput.get(i));
        }

        RawInputStream<S> stream = RawInputStream.newStream(context, consumedInput);
        int totalTokens = stream.size();
        List<Candidate<S>> candidates = new ArrayList<>();
        List<ParsedNode<S>> path = new ArrayList<>();

        try {
            traverseForCompletion(root, stream, path, candidates);
        } catch (CommandException ignored) {
            return Collections.emptyList();
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Candidate<S> best = candidates.stream()
                                    .filter(c -> !hasUnacceptable(c.chain))
                                    .max(Comparator.comparingInt(c -> scoreCandidate(c, totalTokens)))
                                    .orElse(null);
        if (best == null || best.chain.isEmpty()) {
            return Collections.emptyList();
        }
        if (isInsideSecretPath(best.chain)) {
            return Collections.emptyList();
        }

        ParsedNode<S> currentParsed = best.chain.get(best.chain.size() - 1);
        Node<S> currentNode = currentParsed.getDelegate();
        if (!hasSuggestionPermission(context, currentNode.getOriginalPathway())) {
            return Collections.emptyList();
        }

        FlagArgument<S> flagValueTarget = findFlagValueTarget(context, best.chain);
        if (flagValueTarget != null) {
            return filterByPrefix(collectFlagValueSuggestions(context, flagValueTarget), prefix);
        }

        List<String> suggestions = new ArrayList<>();
        addArgumentSuggestions(context, currentParsed, currentNode, suggestions);
        List<String> commandChain = commandChainFromParsedPath(best.chain);
        addFlagNameSuggestions(context, currentNode, commandChain, resolveUsedFlags(best.chain), suggestions);
        return filterByPrefix(suggestions, prefix);
    }

    private boolean hasBlankGap(ArgumentInput fullInput, int completionIndex) {
        for (int i = 0; i < completionIndex && i < fullInput.size(); i++) {
            String token = fullInput.get(i);
            if (token != null && token.isBlank()) {
                return true;
            }
        }

        // Repeated spaces at argument start produce a single blank completion token.
        if (!fullInput.isEmpty() && completionIndex == 0) {
            String current = fullInput.get(0);
            return current != null && current.isBlank();
        }
        return false;
    }

    private String normalizePrefix(@Nullable String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    private void traverseForCompletion(
            Node<S> node,
            RawInputStream<S> stream,
            List<ParsedNode<S>> path,
            List<Candidate<S>> candidates
    ) throws CommandException {
        int saved = stream.getRawIndex();

        ParsedNode<S> parsed = parseNodeForCompletion(node, stream, commandChainForNode(path, node));
        if (parsed == null) {
            stream.setRawIndex(saved);
            return;
        }

        path.add(parsed);
        int afterParse = stream.getRawIndex();

        if (node.isLeaf() || !stream.hasNext()) {
            candidates.add(new Candidate<>(new ArrayList<>(path), afterParse));
        }
        if (stream.hasNext()) {
            for (Node<S> child : sortedChildren(node)) {
                int childSaved = stream.getRawIndex();
                traverseForCompletion(child, stream, path, candidates);
                stream.setRawIndex(childSaved);
            }
        }

        path.remove(path.size() - 1);
        stream.setRawIndex(saved);
    }

    private @Nullable ParsedNode<S> parseNodeForCompletion(
            Node<S> node,
            RawInputStream<S> inputStream,
            List<String> commandChain
    ) {
        Map<String, ParseResult<S>> parseResultMap = new HashMap<>();
        if (!node.isRoot()) {
            ParseResult<S> mainParse = parseArgument(node.main, inputStream);
            if (mainParse.isUnAcceptableScore()) {
                return null;
            }
            parseResultMap.put(node.main.getName(), mainParse);
        }

        Iterator<Argument<S>> optionalsIterator = node.optionals.iterator();

        while (inputStream.hasNext()) {
            String peek = inputStream.next();
            Set<FlagArgument<S>> extracted = extractFlagsForNode(node, peek, commandChain);
            if (!extracted.isEmpty()) {
                boolean hasValueFlags = extracted.stream().anyMatch(flag -> !flag.isSwitch());
                String sharedValueInput = hasValueFlags && inputStream.hasNext() ? inputStream.next() : null;
                for (var extractedFlag : extracted) {
                    parseResultMap.put(
                            extractedFlag.getName(),
                            parseFlagArgument(extractedFlag, peek, sharedValueInput, inputStream)
                    );
                }
                continue;
            }
            if (Patterns.isInputFlag(peek)) {
                inputStream.backward();
            } else {
                inputStream.backward();
            }

            if (!optionalsIterator.hasNext()) {
                break;
            }
            Argument<S> optional = optionalsIterator.next();
            ParseResult<S> optionalParseResult = parseArgument(optional, inputStream);
            parseResultMap.put(optional.getName(), optionalParseResult);
        }

        return new ParsedNode<>(node, parseResultMap);
    }

    private Set<FlagArgument<S>> extractFlagsForNode(Node<S> node, String rawFlagInput, List<String> commandChain) {
        if (!Patterns.isInputFlag(rawFlagInput)) {
            return Collections.emptySet();
        }

        Set<FlagArgument<S>> extracted = new LinkedHashSet<>();
        boolean matched = false;

        for (CommandPathway<S> pathway : effectivePathways(node, commandChain)) {
            try {
                Set<FlagArgument<S>> pathwayExtracted = pathway.getFlagExtractor().extract(rawFlagInput);
                if (!pathwayExtracted.isEmpty()) {
                    matched = true;
                    extracted.addAll(pathwayExtracted);
                }
            } catch (CommandException ignored) {
                // Unknown for this pathway; try remaining pathway scopes.
            }
        }

        return matched ? extracted : Collections.emptySet();
    }

    private List<CommandPathway<S>> effectivePathways(Node<S> node, List<String> commandChain) {
        Set<CommandPathway<S>> scopes = new LinkedHashSet<>();
        scopes.add(node.getOriginalPathway());

        Argument<S> main = node.getMainArgument();
        if (main.isCommand()) {
            Command<S> commandScope = main.asCommand();
            scopes.addAll(commandScope.getDedicatedPathways());
            scopes.add(commandScope.getDefaultPathway());
        }
        scopes.addAll(rootPathwaysForCommandScope(commandChain));

        return new ArrayList<>(scopes);
    }

    private List<String> commandChainForNode(List<ParsedNode<S>> parsedPath, Node<S> node) {
        List<String> chain = commandChainFromParsedPath(parsedPath);
        if (!node.isRoot() && node.getMainArgument().isCommand()) {
            chain.add(node.getMainArgument().asCommand().getName());
        }
        return chain;
    }

    private List<String> commandChainFromParsedPath(List<ParsedNode<S>> parsedPath) {
        List<String> chain = new ArrayList<>();
        for (ParsedNode<S> parsedNode : parsedPath) {
            if (parsedNode.isRoot()) {
                continue;
            }
            Argument<S> main = parsedNode.getMainArgument();
            if (main.isCommand()) {
                chain.add(main.asCommand().getName());
            }
        }
        return chain;
    }

    private List<CommandPathway<S>> rootPathwaysForCommandScope(List<String> commandChain) {
        Set<CommandPathway<S>> rootPathways = new LinkedHashSet<>();
        Command<S> rootCommand = root.getMainArgument().asCommand();
        rootPathways.addAll(rootCommand.getDedicatedPathways());
        rootPathways.add(rootCommand.getDefaultPathway());

        List<CommandPathway<S>> scoped = new ArrayList<>();
        for (CommandPathway<S> pathway : rootPathways) {
            if (isExactCommandScope(pathway, commandChain)) {
                scoped.add(pathway);
            }
        }
        return scoped;
    }

    private boolean isExactCommandScope(CommandPathway<S> pathway, List<String> commandChain) {
        int commandPrefixLength = leadingCommandPrefixLength(pathway);
        if (commandPrefixLength != commandChain.size()) {
            return false;
        }

        List<Argument<S>> arguments = pathway.getArguments();
        for (int i = 0; i < commandChain.size(); i++) {
            Argument<S> argument = arguments.get(i);
            if (!argument.isCommand() || !argument.asCommand().hasName(commandChain.get(i))) {
                return false;
            }
        }
        return true;
    }

    private int leadingCommandPrefixLength(CommandPathway<S> pathway) {
        int count = 0;
        for (Argument<S> argument : pathway.getArguments()) {
            if (!argument.isCommand()) {
                break;
            }
            count++;
        }
        return count;
    }

    private ParseResult<S> parseFlagArgument(
            FlagArgument<S> flag,
            String rawFlagInput,
            @Nullable String sharedValueInput,
            RawInputStream<S> inputStream
    ) {
        if (flag.isSwitch()) {
            return ParseResult.of(flag, rawFlagInput, true, null);
        }

        if (sharedValueInput == null || sharedValueInput.isBlank()) {
            return ParseResult.failedParse(flag, sharedValueInput == null ? "" : sharedValueInput, null);
        }

        var inputType = flag.flagData().inputType();
        if (inputType == null) {
            return ParseResult.failedParse(flag, sharedValueInput, new IllegalStateException("Missing input type for value flag"));
        }
        try {
            Object parsed = inputType.parse(inputStream.getContext(), flag, sharedValueInput);
            return ParseResult.of(flag, sharedValueInput, parsed, null);
        } catch (Throwable error) {
            return ParseResult.of(flag, sharedValueInput, null, error);
        }
    }

    private ParseResult<S> parseArgument(Argument<S> argument, RawInputStream<S> inputStream) {
        StringBuilder builder = new StringBuilder();

        if (argument.isGreedy()) {
            int limit = argument.greedyLimit();
            int consumed = 0;
            while (inputStream.hasNext()) {
                if (limit > 0 && consumed >= limit) {
                    break;
                }
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(inputStream.next());
                consumed++;
            }
        } else {
            final int toConsume = argument.type().getNumberOfParametersToConsume(argument);
            int consumed = 0;
            while (inputStream.hasNext() && consumed < toConsume) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(inputStream.next());
                consumed++;
            }
        }

        final String input = builder.toString();
        if (input.isBlank()) {
            if (argument.isCommand()) {
                return ParseResult.unacceptableParse(argument, input, new IndexOutOfBoundsException());
            }
            return ParseResult.failedParse(argument, input, null);
        }

        try {
            var result = argument.type().parse(inputStream.getContext(), argument, input);
            return ParseResult.of(argument, input, result, null);
        } catch (Throwable error) {
            if (argument.isCommand()) {
                return ParseResult.unacceptableParse(argument, input, error);
            }
            return ParseResult.of(argument, input, null, error);
        }
    }

    private List<Node<S>> sortedChildren(Node<S> node) {
        List<Node<S>> sorted = new ArrayList<>(node.children.values());
        sorted.sort(
                Comparator.comparing((Node<S> child) -> child.getPriority())
                        .thenComparing(child -> child.getMainArgument().getName(), String.CASE_INSENSITIVE_ORDER)
        );
        return sorted;
    }

    private boolean isInsideSecretPath(List<ParsedNode<S>> chain) {
        for (ParsedNode<S> parsedNode : chain) {
            if (parsedNode.isRoot()) {
                continue;
            }
            Argument<S> main = parsedNode.getMainArgument();
            if (main.isCommand() && main.asCommand().isSecret()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSuggestionPermission(SuggestionContext<S> context, CommandPathway<S> pathway) {
        if (context.command().isIgnoringACPerms()) {
            return true;
        }
        return imperatConfig.getPermissionChecker().hasPermission(context.source(), pathway);
    }

    private boolean isSuggestionVisible(
            SuggestionContext<S> context,
            Argument<S> argument,
            @Nullable CommandPathway<S> pathway
    ) {
        if (argument.isCommand() && argument.asCommand().isSecret()) {
            return false;
        }

        if (context.command().isIgnoringACPerms()) {
            return true;
        }

        var checker = imperatConfig.getPermissionChecker();
        if (pathway != null && !checker.hasPermission(context.source(), pathway)) {
            return false;
        }
        return checker.hasPermission(context.source(), argument);
    }

    private void addArgumentSuggestions(
            SuggestionContext<S> context,
            ParsedNode<S> currentParsed,
            Node<S> currentNode,
            List<String> target
    ) {
        Map<String, ParseResult<S>> parseResults = currentParsed.getParseResults();
        Set<String> consumedNames = parseResults.keySet();

        List<Argument<S>> unresolvedOptionals = new ArrayList<>();
        for (Argument<S> optional : currentNode.optionals) {
            if (!consumedNames.contains(optional.getName())) {
                unresolvedOptionals.add(optional);
            }
        }

        boolean overlapEnabled = imperatConfig.isOptionalParameterSuggestionOverlappingEnabled();
        Set<String> seenChildSuggestions = new LinkedHashSet<>();

        if (!unresolvedOptionals.isEmpty()) {
            if (overlapEnabled) {
                for (Argument<S> optional : unresolvedOptionals) {
                    addArgumentProviderSuggestions(context, optional, currentNode.getOriginalPathway(), target, false);
                }
                for (Node<S> child : sortedChildren(currentNode)) {
                    addChildSuggestions(context, child, target, seenChildSuggestions);
                }
            } else {
                addArgumentProviderSuggestions(context, unresolvedOptionals.get(0), currentNode.getOriginalPathway(), target, false);
            }
            return;
        }

        for (Node<S> child : sortedChildren(currentNode)) {
            addChildSuggestions(context, child, target, seenChildSuggestions);
        }
    }

    private void addChildSuggestions(
            SuggestionContext<S> context,
            Node<S> child,
            List<String> target,
            Set<String> seen
    ) {
        Argument<S> main = child.getMainArgument();
        if (!isSuggestionVisible(context, main, child.getOriginalPathway())) {
            return;
        }

        for (String suggestion : provideSuggestions(context, main)) {
            if (suggestion == null || suggestion.isEmpty()) {
                continue;
            }
            if (seen.add(suggestion)) {
                target.add(suggestion);
            }
        }
    }

    private void addArgumentProviderSuggestions(
            SuggestionContext<S> context,
            Argument<S> argument,
            @Nullable CommandPathway<S> pathway,
            List<String> target,
            boolean deduplicate
    ) {
        if (!isSuggestionVisible(context, argument, pathway)) {
            return;
        }

        if (!deduplicate) {
            for (String suggestion : provideSuggestions(context, argument)) {
                if (suggestion != null && !suggestion.isEmpty()) {
                    target.add(suggestion);
                }
            }
            return;
        }

        Set<String> seen = new LinkedHashSet<>();
        for (String suggestion : provideSuggestions(context, argument)) {
            if (suggestion == null || suggestion.isEmpty()) {
                continue;
            }
            if (seen.add(suggestion)) {
                target.add(suggestion);
            }
        }
    }

    private List<String> provideSuggestions(SuggestionContext<S> context, Argument<S> argument) {
        SuggestionProvider<S> provider = argument.getSuggestionResolver();
        if (provider == null) {
            provider = argument.type().getSuggestionProvider();
        }
        if (provider == null) {
            provider = imperatConfig.getDefaultSuggestionResolver();
        }
        if (provider == null) {
            return Collections.emptyList();
        }
        List<String> provided = provider.provide(context, argument);
        return provided == null ? Collections.emptyList() : provided;
    }

    private void addFlagNameSuggestions(
            SuggestionContext<S> context,
            Node<S> currentNode,
            List<String> commandChain,
            Set<String> usedFlags,
            List<String> target
    ) {
        Set<String> seen = new LinkedHashSet<>();
        if ("switchscope".equals(context.command().getName())) {
            var rootCmd = context.command();
            System.out.println("[DBG root dedicated] " + rootCmd.getDedicatedPathways().stream()
                                                                 .map(p -> p.formatted() + " flags=" + p.getFlagExtractor().getRegisteredFlags()
                                                                                                               .stream().map(FlagArgument::getName)
                                                                                                               .toList())
                                                                 .toList());
        }
        for (CommandPathway<S> pathway : effectivePathways(currentNode, commandChain)) {
            if ("example".equals(context.command().getName()) || "git".equals(context.command().getName()) || "switchscope".equals(
                    context.command().getName())) {
                System.out.println("[DBG flags] cmd=" + context.command().getName()
                                           + ", chain=" + commandChain
                                           + ", node=" + currentNode.getMainArgument().getName()
                                           + ", pathway=" + pathway.formatted()
                                           + ", flags=" + pathway.getFlagExtractor().getRegisteredFlags().stream().map(FlagArgument::getName)
                                                                  .toList());
            }
            if (!hasSuggestionPermission(context, pathway)) {
                continue;
            }

            for (FlagArgument<S> flag : pathway.getFlagExtractor().getRegisteredFlags()) {
                if (usedFlags.contains(flag.getName().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (!isSuggestionVisible(context, flag, pathway)) {
                    continue;
                }

                String main = "-" + flag.getName();
                if (seen.add(main)) {
                    target.add(main);
                }
                for (String alias : flag.flagData().aliases()) {
                    String formattedAlias = "-" + alias;
                    if (seen.add(formattedAlias)) {
                        target.add(formattedAlias);
                    }
                }
            }
        }
    }

    private Set<String> resolveUsedFlags(List<ParsedNode<S>> chain) {
        Set<String> used = new LinkedHashSet<>();
        for (ParsedNode<S> parsedNode : chain) {
            for (ParseResult<S> parseResult : parsedNode.getParseResults().values()) {
                Argument<S> argument = parseResult.getArgument();
                if (argument.isFlag()) {
                    used.add(argument.getName().toLowerCase(Locale.ROOT));
                }
            }
        }
        return used;
    }

    private @Nullable FlagArgument<S> findFlagValueTarget(
            SuggestionContext<S> context,
            List<ParsedNode<S>> chain
    ) {
        int completionIndex = context.getArgToComplete().index();
        if (completionIndex <= 0 || completionIndex >= context.arguments().size()) {
            return null;
        }

        String previousRaw = context.arguments().getOr(completionIndex - 1, null);
        if (previousRaw == null || !Patterns.isInputFlag(previousRaw)) {
            return null;
        }

        Node<S> currentNode = chain.get(chain.size() - 1).getDelegate();
        List<String> commandChain = commandChainFromParsedPath(chain);
        for (CommandPathway<S> pathway : effectivePathways(currentNode, commandChain)) {
            if (!hasSuggestionPermission(context, pathway)) {
                continue;
            }
            for (FlagArgument<S> flag : pathway.getFlagExtractor().getRegisteredFlags()) {
                if (flag.flagData().acceptsInput(previousRaw)) {
                    return flag.isSwitch() ? null : flag;
                }
            }
        }
        return null;
    }

    private List<String> collectFlagValueSuggestions(
            SuggestionContext<S> context,
            FlagArgument<S> flag
    ) {
        SuggestionProvider<S> provider = flag.inputSuggestionResolver();
        if (provider != null) {
            List<String> result = provider.provide(context, flag);
            return result == null ? Collections.emptyList() : result;
        }

        var inputType = flag.flagData().inputType();
        if (inputType == null) {
            return Collections.emptyList();
        }

        SuggestionProvider<S> inputTypeProvider = inputType.getSuggestionProvider();
        if (inputTypeProvider == null) {
            return Collections.emptyList();
        }
        List<String> result = inputTypeProvider.provide(context, flag);
        return result == null ? Collections.emptyList() : result;
    }

    private List<String> filterByPrefix(List<String> suggestions, String prefix) {
        if (prefix.isEmpty()) {
            return suggestions;
        }
        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion == null || suggestion.isEmpty()) {
                continue;
            }
            if (startsWithIgnoreCase(suggestion, prefix)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

    private boolean startsWithIgnoreCase(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    @Override
    public HelpResult<S> queryHelp(@NotNull HelpQuery<S> query) {
        throw new UnsupportedOperationException("queryHelp not yet implemented");
    }

    // ------------------------------------------------------------------
    // Internal candidate record
    // ------------------------------------------------------------------

    private record Candidate<S extends CommandSource>(List<ParsedNode<S>> chain, int consumedIndex) {
    }
}
