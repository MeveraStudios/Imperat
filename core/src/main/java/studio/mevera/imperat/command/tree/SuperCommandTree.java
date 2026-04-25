package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandContext;
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
        this.root.addTerminalPathway(command.getDefaultPathway());
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
        for (Node<S> child : node.children) {
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

            Node<S> child = current.children.stream().filter((n) -> n.main.getName().equals(arg.getName())).findFirst().orElse(null);
            if (child == null) {
                child = new Node<>(current, usage, arg);
                current.children.add(child);
                size++;
            }
            current = child;
        }
        // trailing optionals (no more required args following)
        attachOptionals(current, pendingOptionals);
        current.addTerminalPathway(usage);
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
        target.children.add(subRoot);
        subRoot.parent = target;
        size += subTreeSize(subTree);
    }

    private @Nullable Node<S> findByFormat(Node<S> node, String format) {
        if (node.main.format().equals(format)
                    || node.optionals.stream().anyMatch(optional -> optional.format().equals(format))) {
            return node;
        }
        for (Node<S> child : node.children) {
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
     * @return the best matching tree branch plus the exact command pathway it resolved to.
     */
    @Override
    public @NotNull CommandTreeMatch<S> execute(ExecutionContext<S> context, @NotNull ArgumentInput input)
            throws CommandException {
        RawInputStream<S> stream = RawInputStream.newStream(context, input);
        int totalTokens = stream.size();

        List<Candidate<S>> candidates = new ArrayList<>();
        List<ParsedNode<S>> path = new ArrayList<>();
        traverse(root, stream, path, candidates);

        if (candidates.isEmpty()) {
            return CommandTreeMatch.empty(root.getMainArgument().asCommand());
        }

        Candidate<S> best = candidates.stream()
                                    .filter(c -> !hasUnacceptable(c.chain))
                                    .max(Comparator.comparingInt(c -> scoreCandidate(c, totalTokens)))
                                    .orElse(null);

        return best == null
                       ? CommandTreeMatch.empty(root.getMainArgument().asCommand())
                       : new CommandTreeMatch<>(best.chain, best.command, best.pathway);
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

            // Eagerly bind any optionals/inline flags that root owns BEFORE descending
            // into children. This mirrors Node.parseArgument's behaviour for normal
            // nodes so root-attached optionals (e.g. @Execute root pathway with a
            // single optional arg) are resolvable as a valid match for partial input.
            int rootSaved = stream.getRawIndex();
            Map<String, ParseResult<S>> rootResults = new HashMap<>();
            node.parseOptionalsAndFlags(stream, rootResults);
            int afterRootParse = stream.getRawIndex();

            ParsedNode<S> rootParsed = new ParsedNode<>(node, rootResults);
            path.add(rootParsed);

            // root is a valid candidate as soon as it has consumed everything (or
            // there is nothing left). Anything still in the stream beyond root's
            // optionals is for the children, so partial root matches are skipped.
            addExecutionCandidates(node, stream, path, candidates, afterRootParse, false);
            if (!node.isLeaf()) {
                for (Node<S> child : node.children) {
                    int saved = stream.getRawIndex();
                    traverse(child, stream, path, candidates);
                    stream.setRawIndex(saved);
                }
            }
            path.remove(path.size() - 1);
            stream.setRawIndex(rootSaved);
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
            addExecutionCandidates(node, stream, path, candidates, afterParse, true);
        } else if (!stream.hasNext()) {
            // input exhausted mid-tree: still a candidate (lower-scoring, but may be the best)
            addExecutionCandidates(node, stream, path, candidates, afterParse, true);
        } else {
            for (Node<S> child : node.children) {
                int childSaved = stream.getRawIndex();
                traverse(child, stream, path, candidates);
                stream.setRawIndex(childSaved);
            }
        }

        path.remove(path.size() - 1);
        stream.setRawIndex(saved);
    }

    private void addExecutionCandidates(
            Node<S> node,
            RawInputStream<S> stream,
            List<ParsedNode<S>> path,
            List<Candidate<S>> candidates,
            int consumedIndex,
            boolean allowPartial
    ) {
        Command<S> terminalCommand = resolveTerminalCommand(path);
        for (CommandPathway<S> pathway : candidatePathwaysForNode(node)) {
            int consumedWithFlags = consumeRemainingFlags(pathway, stream, consumedIndex);
            boolean fullyConsumed = consumedWithFlags + 1 >= stream.size();
            if (!allowPartial && stream.hasNext() && !fullyConsumed) {
                continue;
            }

            Command<S> commandScope = executionCommandForPathway(pathway, terminalCommand);
            candidates.add(new Candidate<>(
                    new ArrayList<>(path),
                    consumedWithFlags,
                    commandScope,
                    pathway
            ));
        }
    }

    private List<CommandPathway<S>> candidatePathwaysForNode(Node<S> node) {
        List<CommandPathway<S>> pathways = new ArrayList<>();
        for (CommandPathway<S> pathway : node.getTerminalPathways()) {
            addPathwayScope(pathways, pathway);
        }
        if (pathways.isEmpty()) {
            addPathwayScope(pathways, node.getOriginalPathway());
        }
        return pathways;
    }

    @SuppressWarnings("unchecked")
    private Command<S> resolveTerminalCommand(List<ParsedNode<S>> path) {
        Command<S> resolved = root.getMainArgument().asCommand();
        for (ParsedNode<S> parsedNode : path) {
            Argument<S> main = parsedNode.getMainArgument();
            if (!main.isCommand()) {
                continue;
            }

            ParseResult<S> parseResult = parsedNode.getParseResults().get(main.getName());
            if (parseResult != null && parseResult.getParsedValue() instanceof Command<?> parsedCommand) {
                resolved = (Command<S>) parsedCommand;
                continue;
            }

            if (!parsedNode.isRoot()) {
                resolved = main.asCommand();
            }
        }
        return resolved;
    }

    private Command<S> executionCommandForPathway(CommandPathway<S> pathway, Command<S> terminalCommand) {
        Argument<S> first = pathway.getArgumentAt(0);
        if (first != null && first.isCommand()) {
            return root.getMainArgument().asCommand();
        }
        return terminalCommand;
    }

    private int consumeRemainingFlags(CommandPathway<S> pathway, RawInputStream<S> stream, int consumedIndex) {
        RawInputStream<S> remaining = stream.copyAndSetAtIndex(consumedIndex);
        int lastConsumed = consumedIndex;

        while (remaining.hasNext()) {
            String raw = remaining.next();
            if (!Patterns.isInputFlag(raw)) {
                return lastConsumed;
            }

            Set<FlagArgument<S>> extracted;
            try {
                extracted = pathway.getFlagExtractor().extract(raw);
            } catch (CommandException ignored) {
                return lastConsumed;
            }
            if (extracted.isEmpty()) {
                return lastConsumed;
            }

            lastConsumed = remaining.getRawIndex();
            boolean hasValueFlags = extracted.stream().anyMatch(flag -> !flag.isSwitch());
            if (hasValueFlags) {
                if (!remaining.hasNext()) {
                    return consumedIndex;
                }
                remaining.next();
                lastConsumed = remaining.getRawIndex();
            }
        }
        return lastConsumed;
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
     *   <li>Fewest failed-parse arguments — a clean (zero-failure) match always
     *       wins over any partially-failed match, regardless of depth. Among
     *       equally-failed branches the deeper / more-matched one wins, which is
     *       what error reporting wants for "closest usage".</li>
     *   <li>Number of matched command literals (subcommand depth).</li>
     *   <li>Full input consumption.</li>
     *   <li>Sum of per-argument parse scores.</li>
     *   <li>Depth of the chain.</li>
     *   <li>Pathway-level tweaks (default/method/flag preferences).</li>
     * </ol>
     */
    private int scoreCandidate(Candidate<S> candidate, int totalTokens) {
        int consumed = candidate.consumedIndex + 1; // raw index is -1 at start
        boolean fullyConsumed = totalTokens == 0 || consumed >= totalTokens;

        int totalScore = 0;
        int failures = 0;
        int commandLiterals = 0;
        int depth = candidate.chain.size();
        for (ParsedNode<S> pn : candidate.chain) {
            if (!pn.isRoot() && pn.getMainArgument().isCommand()) {
                commandLiterals++;
            }
            totalScore += pn.getTotalParseScore();
            for (ParseResult<S> r : pn.getParseResults().values()) {
                if (r.isFailureScore()) {
                    failures++;
                }
            }
        }

        // A failure is dominant: one missed required argument should never beat a
        // pathway that consumed everything cleanly, even if the failed branch is
        // deeper. The penalty must outweigh every other tier combined.
        return -failures * 100_000_000
                       + commandLiterals * 2_000_000
                       + (fullyConsumed ? 1_000_000 : 0)
                       + totalScore * 1_000
                       + depth * 10
                       + scoreCandidatePathway(candidate.pathway);
    }

    private int scoreCandidatePathway(@Nullable CommandPathway<S> pathway) {
        if (pathway == null) {
            return 0;
        }

        int score = pathway.getFlagExtractor().getRegisteredFlags().size() * 15;
        if (pathway.isDefault()) {
            score -= 20;
        }
        if (pathway.isDefault() && pathway.getMethodElement() == null) {
            score -= 400;
        }
        return score;
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
            candidates.add(Candidate.completion(new ArrayList<>(path), afterParse));
        }
        if (stream.hasNext()) {
            for (Node<S> child : node.getChildren()) {
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
        List<CommandPathway<S>> scopes = new ArrayList<>();
        addPathwayScope(scopes, node.getOriginalPathway());

        Argument<S> main = node.getMainArgument();
        if (main.isCommand()) {
            Command<S> commandScope = main.asCommand();
            for (CommandPathway<S> pathway : commandScope.getDedicatedPathways()) {
                addPathwayScope(scopes, pathway);
            }
            addPathwayScope(scopes, commandScope.getDefaultPathway());
        }
        for (CommandPathway<S> pathway : rootPathwaysForCommandScope(commandChain)) {
            addPathwayScope(scopes, pathway);
        }

        return scopes;
    }

    private void addPathwayScope(List<CommandPathway<S>> scopes, @Nullable CommandPathway<S> pathway) {
        if (pathway == null) {
            return;
        }
        for (CommandPathway<S> existing : scopes) {
            if (existing == pathway) {
                return;
            }
        }
        scopes.add(pathway);
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
        List<CommandPathway<S>> rootPathways = new ArrayList<>();
        Command<S> rootCommand = root.getMainArgument().asCommand();
        for (CommandPathway<S> pathway : rootCommand.getDedicatedPathways()) {
            addPathwayScope(rootPathways, pathway);
        }
        addPathwayScope(rootPathways, rootCommand.getDefaultPathway());

        List<CommandPathway<S>> scoped = new ArrayList<>();
        for (CommandPathway<S> pathway : rootPathways) {
            if (isExactCommandScope(pathway, commandChain)) {
                addPathwayScope(scoped, pathway);
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

        if (isGreedyArgument(argument)) {
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
                for (Node<S> child : currentNode.getChildren()) {
                    addChildSuggestions(context, child, target, seenChildSuggestions);
                }
            } else {
                addArgumentProviderSuggestions(context, unresolvedOptionals.get(0), currentNode.getOriginalPathway(), target, false);
            }
            return;
        }

        for (Node<S> child : currentNode.getChildren()) {
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
        for (CommandPathway<S> pathway : effectivePathways(currentNode, commandChain)) {
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
        if (query.getLimit() <= 0 || query.getMaxDepth() < 0) {
            return HelpResult.empty();
        }

        Command<S> rootCommand = root.getMainArgument().asCommand();
        if (rootCommand.isSecret()) {
            return HelpResult.empty();
        }

        List<HelpEntry<S>> entries = new ArrayList<>();
        Set<String> seenUsages = new LinkedHashSet<>();
        List<Command<S>> visitedCommands = new ArrayList<>();
        collectHelpEntries(rootCommand, rootCommand, query, visitedCommands, seenUsages, entries);
        return HelpResult.copyOf(entries);
    }

    @Override
    public @NotNull CommandPathway<S> getClosestPathwayToContext(CommandContext<S> context, CommandTreeMatch<S> treeMatch) {

        //we find shortest executable node and get its pathway
        var parsedNodesList = treeMatch.parsedNodes();
        ParsedNode<S> node = parsedNodesList.get(parsedNodesList.size() - 1);
        CommandPathway<S> pathway = node.originalPathway;

        List<Node<S>> path = traverse(new ArrayList<>(), node.getDelegate(), context);
        return path.isEmpty() ? pathway : path.get(path.size() - 1).originalPathway;
    }

    private List<Node<S>> traverse(List<Node<S>> path, Node<S> node, CommandContext<S> context) {

        if (!node.isRoot()) {
            path.add(node);
        }

        if (node.isLeaf()) {
            return path;
        }

        var topChild = node.getChildren().iterator().next();
        return traverse(path, topChild, context);
    }

    private void collectHelpEntries(
            Command<S> rootCommand,
            Command<S> command,
            HelpQuery<S> query,
            List<Command<S>> visitedCommands,
            Set<String> seenUsages,
            List<HelpEntry<S>> entries
    ) {
        if (entries.size() >= query.getLimit() || hasVisited(visitedCommands, command) || isSecretCommandPath(command, rootCommand)) {
            return;
        }

        visitedCommands.add(command);
        for (CommandPathway<S> pathway : command.getDedicatedPathways()) {
            if (entries.size() >= query.getLimit()) {
                return;
            }
            addHelpEntry(rootCommand, command, pathway, query, seenUsages, entries);
        }

        for (Command<S> child : command.getSubCommands()) {
            collectHelpEntries(rootCommand, child, query, visitedCommands, seenUsages, entries);
            if (entries.size() >= query.getLimit()) {
                return;
            }
        }
    }

    private void addHelpEntry(
            Command<S> rootCommand,
            Command<S> ownerCommand,
            CommandPathway<S> pathway,
            HelpQuery<S> query,
            Set<String> seenUsages,
            List<HelpEntry<S>> entries
    ) {
        if (containsSecretCommand(pathway)) {
            return;
        }

        CommandPathway<S> helpPathway = createHelpPathway(rootCommand, ownerCommand, pathway);
        if (helpPathway.size() == 0 && !query.getRootUsagePredicate().test(pathway)) {
            return;
        }
        if (helpPathway.size() > query.getMaxDepth()) {
            return;
        }
        if (!passesHelpFilters(helpPathway, query)) {
            return;
        }

        HelpEntry<S> entry = HelpEntry.of(helpPathway);
        if (seenUsages.add(entry.getUsage())) {
            entries.add(entry);
        }
    }

    private CommandPathway<S> createHelpPathway(
            Command<S> rootCommand,
            Command<S> ownerCommand,
            CommandPathway<S> pathway
    ) {
        List<Argument<S>> helpArguments = new ArrayList<>();
        addOwnerCommandPrefix(rootCommand, ownerCommand, helpArguments);
        for (Argument<S> argument : pathway.getArguments()) {
            helpArguments.add(copyHelpArgument(rootCommand, argument));
        }

        return CommandPathway.<S>builder(pathway.getMethodElement())
                       .arguments(helpArguments)
                       .withFlags(pathway.getFlagExtractor().getRegisteredFlags())
                       .examples(pathway.getExamples())
                       .execute(pathway.getExecution())
                       .permission(pathway.getPermissionsData())
                       .description(pathway.getDescription())
                       .coordinator(pathway.getCoordinator())
                       .cooldown(pathway.getCooldown())
                       .build(rootCommand);
    }

    private void addOwnerCommandPrefix(
            Command<S> rootCommand,
            Command<S> ownerCommand,
            List<Argument<S>> target
    ) {
        List<Command<S>> prefixes = new ArrayList<>();
        Command<S> current = ownerCommand;
        while (current != null && current != rootCommand) {
            prefixes.add(0, current);
            current = current.getParent();
        }

        for (Command<S> prefix : prefixes) {
            target.add(copyCommandLiteral(rootCommand, prefix));
        }
    }

    private Argument<S> copyCommandLiteral(Command<S> rootCommand, Command<S> command) {
        List<String> aliases = command.aliases();
        String[] names = new String[aliases.size() + 1];
        names[0] = command.getName();
        for (int i = 0; i < aliases.size(); i++) {
            names[i + 1] = aliases.get(i);
        }
        return Argument.literal(rootCommand.imperat(), names);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Argument<S> copyHelpArgument(Command<S> rootCommand, Argument<S> argument) {
        if (argument.isCommand()) {
            return copyCommandLiteral(rootCommand, argument.asCommand());
        }

        Argument<S> copy = Argument.of(
                argument.getName(),
                (ArgumentType) argument.type(),
                argument.getPermissionsData(),
                argument.getDescription(),
                argument.isOptional(),
                isGreedyArgument(argument),
                argument.getDefaultValueSupplier(),
                argument.getSuggestionResolver(),
                argument.getValidators().toList()
        );
        copy.setFormat(argument.format());
        return copy;
    }

    private boolean passesHelpFilters(CommandPathway<S> pathway, HelpQuery<S> query) {
        for (var filter : query.getFilters()) {
            if (!filter.filter(pathway)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsSecretCommand(CommandPathway<S> pathway) {
        for (Argument<S> argument : pathway.getArguments()) {
            if (argument.isCommand() && argument.asCommand().isSecret()) {
                return true;
            }
        }
        return false;
    }

    private boolean isSecretCommandPath(Command<S> command, Command<S> rootCommand) {
        Command<S> current = command;
        while (current != null && current != rootCommand) {
            if (current.isSecret()) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean hasVisited(List<Command<S>> visitedCommands, Command<S> command) {
        for (Command<S> visited : visitedCommands) {
            if (visited == command) {
                return true;
            }
        }
        return false;
    }

    private boolean isGreedyArgument(Argument<S> argument) {
        return argument.isGreedy() || argument.type().isGreedy(argument);
    }

    // ------------------------------------------------------------------
    // Internal candidate record
    // ------------------------------------------------------------------

    private record Candidate<S extends CommandSource>(
            List<ParsedNode<S>> chain,
            int consumedIndex,
            @Nullable Command<S> command,
            @Nullable CommandPathway<S> pathway
    ) {

        static <S extends CommandSource> Candidate<S> completion(List<ParsedNode<S>> chain, int consumedIndex) {
            return new Candidate<>(chain, consumedIndex, null, null);
        }
    }
}
