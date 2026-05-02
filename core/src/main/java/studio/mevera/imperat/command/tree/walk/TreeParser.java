package studio.mevera.imperat.command.tree.walk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.tree.CommandTreeMatch;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.command.tree.ParseResult;
import studio.mevera.imperat.command.tree.ParsedNode;
import studio.mevera.imperat.command.tree.RawInputStream;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.priority.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy responsible for executing a command tree against raw input — the
 * DFS walk that produces the best-matching {@link CommandTreeMatch}. Extracted
 * from the former {@code SuperCommandTree}.
 *
 * <h3>Failure surfacing</h3>
 *
 * <p>The walk treats every node-or-subtree it explores as an
 * {@link ExploreResult}: either it produced at least one viable
 * {@link Candidate}, or it failed entirely with a captured
 * {@link CommandException}. The decision rule mirrors the user's mental
 * model:</p>
 *
 * <ul>
 *   <li>If a parse fails and there is no other branch the walker can try,
 *       the captured error is surfaced immediately — the user sees the
 *       actual rejection reason, not a contextless
 *       {@link studio.mevera.imperat.exception.InvalidSyntaxException}.</li>
 *   <li>If multiple sibling branches exist and at least one produces
 *       candidates, those candidates win and the throws from siblings are
 *       discarded (the alternative succeeded — no need to surface failures
 *       from ones that didn't apply).</li>
 *   <li>If multiple siblings all fail, the walker picks the
 *       <em>most-informative</em> failure as the winner: deepest-consumed
 *       first (the branch that absorbed the most input before choking),
 *       then highest argument-type {@link Priority} (specific types beat
 *       catch-all ones — same ordering used by {@code AmbiguityChecker}
 *       and {@code PriorityList}), then earliest registration order
 *       (deterministic).</li>
 * </ul>
 *
 * <p>The "no siblings" case is just the degenerate "all siblings failed"
 * with N=1 — the only failure trivially wins. Same code path; no special
 * branching.</p>
 */
public final class TreeParser<S extends CommandSource> {

    private final Node<S> root;

    public TreeParser(@NotNull Node<S> root) {
        this.root = root;
    }

    private static CommandException wrapAsCommandException(Throwable t) {
        if (t instanceof CommandException ce) {
            return ce;
        }
        String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        return new CommandException(message, t);
    }

    public @NotNull CommandTreeMatch<S> execute(ExecutionContext<S> context, @NotNull ArgumentInput input)
            throws CommandException {
        RawInputStream<S> stream = RawInputStream.newStream(context, input);
        int totalTokens = stream.size();

        List<Candidate<S>> candidates = new ArrayList<>();
        List<ParsedNode<S>> path = new ArrayList<>();
        ExploreResult<S> outcome = traverse(root, stream, path, candidates, 0);

        // Subtree could not produce a single viable candidate AND captured
        // a concrete failure reason on the way down — surface that instead
        // of falling through to the dispatcher's contextless InvalidSyntax
        // fallback. This is the "throw the actual reason" half of the rule.
        if (outcome instanceof Failed<S> failed && candidates.isEmpty()) {
            throw failed.error();
        }

        if (candidates.isEmpty()) {
            return CommandTreeMatch.empty(root.getMainArgument().asCommand());
        }

        Candidate<S> best = candidates.stream()
                                    .filter(c -> !WalkScoring.hasUnacceptable(c.chain()))
                                    .max(Comparator.comparingInt(c -> WalkScoring.scoreCandidate(c, totalTokens)))
                                    .orElse(null);

        if (best == null) {
            return CommandTreeMatch.empty(root.getMainArgument().asCommand());
        }
        return new CommandTreeMatch<>(
                best.chain(),
                best.trailingFlagResults(),
                best.command(),
                best.pathway(),
                best.consumedIndex()
        );
    }

    /**
     * Picks the most-informative failure from a list of sibling
     * outcomes. Ordering: deepest-consumed first, then highest priority
     * (matching the natural-descending order in
     * {@code Priority.compareTo}), then lowest registration index.
     */
    private Failed<S> pickWinner(List<Failed<S>> failures) {
        Failed<S> winner = failures.get(0);
        for (int i = 1; i < failures.size(); i++) {
            Failed<S> candidate = failures.get(i);
            if (isBetterFailure(candidate, winner)) {
                winner = candidate;
            }
        }
        return winner;
    }

    private boolean isBetterFailure(Failed<S> a, Failed<S> b) {
        if (a.consumedDepth() != b.consumedDepth()) {
            return a.consumedDepth() > b.consumedDepth();
        }
        // Priority.compareTo orders descending (higher priority is "less"),
        // so a < b means a is higher priority.
        int prioCmp = a.priority().compareTo(b.priority());
        if (prioCmp != 0) {
            return prioCmp < 0;
        }
        return a.registrationIndex() < b.registrationIndex();
    }

    private ExploreResult<S> traverse(
            Node<S> node,
            RawInputStream<S> stream,
            List<ParsedNode<S>> path,
            List<Candidate<S>> candidates,
            int siblingIndex
    ) throws CommandException {

        if (node.isRoot()) {
            return traverseRoot(node, stream, path, candidates);
        }

        int saved = stream.getRawIndex();
        ParsedNode<S> parsed;
        try {
            parsed = node.parseArgument(stream);
        } catch (studio.mevera.imperat.exception.DuplicateFlagException dup) {
            // Duplicate-flag is always a hard user error and there is no
            // sibling that could rescue (the duplicate is already on the
            // input). Surface immediately.
            throw dup;
        } catch (CommandException ce) {
            // drainLeadingFlags / parseOptionalsAndFlags raised a typed
            // command exception during the node's parse phase. Treat the
            // whole branch as failed and let the parent decide whether a
            // sibling can rescue.
            int depth = stream.getRawIndex() - saved;
            stream.setRawIndex(saved);
            return new Failed<>(ce, depth, priorityOf(node), siblingIndex);
        } catch (Throwable t) {
            // Unmarked exception (NumberFormatException, IllegalArgumentException,
            // plugin-defined). Treat as "this branch did not fit" — same
            // semantic as before, but carry the throwable so a parent with
            // no other rescue still has SOMETHING to surface instead of a
            // generic InvalidSyntax.
            int depth = stream.getRawIndex() - saved;
            stream.setRawIndex(saved);
            return new Failed<>(wrapAsCommandException(t), depth, priorityOf(node), siblingIndex);
        }
        if (parsed == null) {
            // Subcommand-literal mismatch (unacceptable parse). No concrete
            // user error to attach; the parent will only surface this if
            // every sibling also failed and no better candidate emerged.
            int depth = stream.getRawIndex() - saved;
            stream.setRawIndex(saved);
            return new Failed<>(
                    new CommandException("Subcommand '%s' did not match", node.getMainArgument().format()),
                    depth, priorityOf(node), siblingIndex
            );
        }

        // Main parse may have stored a failure inside the ParsedNode without
        // throwing (the legacy capture-and-defer behaviour). Detect that and
        // treat as branch failure so the user-facing error reaches the
        // surfacing path.
        ParseResult<S> mainResult = parsed.getParseResults().get(node.getMainArgument().getName());
        if (mainResult != null && mainResult.isFailureScore() && mainResult.getError() != null) {
            int depth = stream.getRawIndex() - saved;
            stream.setRawIndex(saved);
            return new Failed<>(
                    wrapAsCommandException(mainResult.getError()),
                    depth, priorityOf(node), siblingIndex
            );
        }

        path.add(parsed);
        int afterParse = stream.getRawIndex();
        int candidateCountBefore = candidates.size();

        ExploreResult<S> result;
        if (node.isLeaf()) {
            addExecutionCandidates(node, stream, path, candidates, afterParse, allowsLeafPartialCandidate(node));
            result = candidates.size() > candidateCountBefore
                             ? new Produced<>()
                             : new Failed<>(
                    new CommandException("Branch parsed but rejected by trailing-input filter"),
                    afterParse - saved, priorityOf(node), siblingIndex
            );
        } else if (!stream.hasNext()) {
            addExecutionCandidates(node, stream, path, candidates, afterParse, true);
            result = candidates.size() > candidateCountBefore
                             ? new Produced<>()
                             : new Failed<>(
                    new CommandException("Branch reached end-of-input but admitted no pathway"),
                    afterParse - saved, priorityOf(node), siblingIndex
            );
        } else {
            result = exploreChildren(node, stream, path, candidates, candidateCountBefore, afterParse - saved, siblingIndex);
        }

        path.remove(path.size() - 1);
        stream.setRawIndex(saved);
        return result;
    }

    /**
     * Iterates a node's children, collects per-sibling outcomes, and
     * applies the "all siblings failed" rule. If at least one child
     * produced candidates the walk succeeds. Otherwise the most-
     * informative child failure is bubbled up.
     */
    private ExploreResult<S> exploreChildren(
            Node<S> node,
            RawInputStream<S> stream,
            List<ParsedNode<S>> path,
            List<Candidate<S>> candidates,
            int candidateCountBefore,
            int parentDepth,
            int parentSiblingIndex
    ) throws CommandException {
        List<Failed<S>> childFailures = new ArrayList<>();
        int childIdx = 0;
        for (Node<S> child : node.getChildren()) {
            int childSaved = stream.getRawIndex();
            ExploreResult<S> childOutcome = traverse(child, stream, path, candidates, childIdx);
            stream.setRawIndex(childSaved);
            if (childOutcome instanceof Failed<S> failed) {
                childFailures.add(failed);
            }
            childIdx++;
        }
        if (candidates.size() > candidateCountBefore) {
            // At least one child rescued the branch — sibling failures are
            // discarded as expected.
            return new Produced<>();
        }
        if (childFailures.isEmpty()) {
            // No children, no candidates. Surface a structural failure so
            // the parent has something to bubble.
            return new Failed<>(
                    new CommandException("No child branches were tried"),
                    parentDepth, priorityOf(node), parentSiblingIndex
            );
        }
        return pickWinner(childFailures);
    }

    /**
     * Root-node traversal mirrors a non-root node but accounts for the
     * two distinct "shapes" the root can produce candidates in: with no
     * arguments (default pathway, empty input) and with positional
     * optionals/flags drained directly off the root. Both shapes
     * iterate the same children for subcommand dispatch; failures are
     * aggregated across the whole exploration before deciding.
     */
    private ExploreResult<S> traverseRoot(
            Node<S> node,
            RawInputStream<S> stream,
            List<ParsedNode<S>> path,
            List<Candidate<S>> candidates
    ) throws CommandException {
        int rootSaved = stream.getRawIndex();
        int candidateCountBefore = candidates.size();
        List<Failed<S>> failures = new ArrayList<>();

        // Shape 1: bare-root candidate + subcommand exploration.
        ParsedNode<S> rootParsed = ParsedNode.of(node, new HashMap<>());
        path.add(rootParsed);
        addExecutionCandidates(node, stream, path, candidates, rootSaved, false);
        if (!node.isLeaf() && stream.hasNext()) {
            int childIdx = 0;
            for (Node<S> child : node.getChildren()) {
                int saved = stream.getRawIndex();
                ExploreResult<S> childOutcome = traverse(child, stream, path, candidates, childIdx);
                stream.setRawIndex(saved);
                if (childOutcome instanceof Failed<S> failed) {
                    failures.add(failed);
                }
                childIdx++;
            }
        }
        path.remove(path.size() - 1);

        // Shape 2: root-level optionals + subcommand exploration off the
        // post-optionals position.
        stream.setRawIndex(rootSaved);
        Map<String, ParseResult<S>> rootResults = new HashMap<>();
        try {
            node.parseOptionalsAndFlags(stream, rootResults);
        } catch (studio.mevera.imperat.exception.DuplicateFlagException dup) {
            throw dup;
        } catch (CommandException ce) {
            int depth = stream.getRawIndex() - rootSaved;
            stream.setRawIndex(rootSaved);
            failures.add(new Failed<>(ce, depth, priorityOf(node), 0));
            return decideRootOutcome(candidates, candidateCountBefore, failures, node);
        }
        int afterRootParse = stream.getRawIndex();
        if (!rootResults.isEmpty() || afterRootParse != rootSaved) {
            ParsedNode<S> rootWithOptionals = ParsedNode.of(node, rootResults);
            path.add(rootWithOptionals);
            if (node.isLeaf() || hasExecutableTerminal(node)) {
                addExecutionCandidates(node, stream, path, candidates, afterRootParse, node.isLeaf());
            }
            if (!node.isLeaf() && stream.hasNext()) {
                int childIdx = 0;
                for (Node<S> child : node.getChildren()) {
                    int saved = stream.getRawIndex();
                    ExploreResult<S> childOutcome = traverse(child, stream, path, candidates, childIdx);
                    stream.setRawIndex(saved);
                    if (childOutcome instanceof Failed<S> failed) {
                        failures.add(failed);
                    }
                    childIdx++;
                }
            }
            path.remove(path.size() - 1);
        }
        stream.setRawIndex(rootSaved);
        return decideRootOutcome(candidates, candidateCountBefore, failures, node);
    }

    private ExploreResult<S> decideRootOutcome(
            List<Candidate<S>> candidates,
            int candidateCountBefore,
            List<Failed<S>> failures,
            Node<S> rootNode
    ) {
        if (candidates.size() > candidateCountBefore) {
            return new Produced<>();
        }
        if (failures.isEmpty()) {
            return new Failed<>(
                    new CommandException("Root produced no candidates and no children were tried"),
                    0, priorityOf(rootNode), 0
            );
        }
        return pickWinner(failures);
    }

    private Priority priorityOf(Node<S> node) {
        Argument<S> main = node.getMainArgument();
        return main == null ? Priority.NORMAL : main.type().getPriority();
    }

    /**
     * Outcome of exploring a single node-rooted subtree. Sealed so
     * exhaustive switches stay future-proof and the type carries the
     * structural distinction between "we got somewhere" and "we hit a
     * wall and here's why".
     */
    private sealed interface ExploreResult<S extends CommandSource> permits Produced, Failed {

    }

    /**
     * The subtree contributed at least one viable {@link Candidate} to the
     * shared candidate list. The walker keeps going; any failures captured
     * within the subtree are harmless ambient noise (a sibling rescued).
     */
    private record Produced<S extends CommandSource>() implements ExploreResult<S> {

    }

    /**
     * The subtree produced zero candidates and exits with a captured
     * failure to bubble up. Carries the metadata needed by
     * {@link #pickWinner(List)} to break ties when several sibling
     * subtrees all failed.
     *
     * @param error              the exception to surface if no other
     *                           branch rescues at this level
     * @param consumedDepth      how many input tokens were absorbed
     *                           before the failure — primary tiebreak,
     *                           higher wins ("you almost had it")
     * @param priority           argument-type {@link Priority} at the
     *                           failing node — secondary tiebreak,
     *                           higher wins (specific types beat
     *                           catch-all)
     * @param registrationIndex  sibling index at the parent's children
     *                           ordering — final tiebreak, lower wins
     *                           (deterministic, first-registered)
     */
    private record Failed<S extends CommandSource>(
            CommandException error,
            int consumedDepth,
            Priority priority,
            int registrationIndex
    ) implements ExploreResult<S> {

    }

    private boolean allowsLeafPartialCandidate(Node<S> node) {
        if (node.isGreedy() || !node.getOptionalArguments().isEmpty()) {
            return true;
        }
        for (CommandPathway<S> pathway : candidatePathwaysForNode(node)) {
            for (Argument<S> argument : pathway) {
                if (argument.isOptional()) {
                    return true;
                }
            }
        }
        return false;
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
            TrailingFlags<S> trailing = consumeRemainingFlags(pathway, stream, consumedIndex);
            int consumedWithFlags = trailing.consumedIndex();
            boolean fullyConsumed = consumedWithFlags + 1 >= stream.size();
            if (!allowPartial && stream.hasNext() && !fullyConsumed) {
                continue;
            }

            Command<S> commandScope = executionCommandForPathway(pathway, terminalCommand);
            candidates.add(new Candidate<>(
                    new ArrayList<>(path),
                    trailing.results(),
                    consumedWithFlags,
                    commandScope,
                    pathway
            ));
        }
    }

    private List<CommandPathway<S>> candidatePathwaysForNode(Node<S> node) {
        List<CommandPathway<S>> pathways = new ArrayList<>();
        if (node.isRoot()) {
            CommandPathway<S> currentDefault = root.getMainArgument().asCommand().getDefaultPathway();
            addPathwayScope(pathways, currentDefault);
            for (CommandPathway<S> pathway : node.getTerminalPathways()) {
                if (pathway.isDefault() && pathway != currentDefault) {
                    continue;
                }
                addPathwayScope(pathways, pathway);
            }
            return pathways;
        }

        for (CommandPathway<S> pathway : node.getTerminalPathways()) {
            addPathwayScope(pathways, pathway);
        }
        if (pathways.isEmpty()) {
            addPathwayScope(pathways, node.getOriginalPathway());
        }
        return pathways;
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

    private boolean hasExecutableTerminal(Node<S> node) {
        for (CommandPathway<S> pathway : node.getTerminalPathways()) {
            if (pathway.getMethodElement() != null) {
                return true;
            }
        }
        return false;
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

    /**
     * Walks input from {@code consumedIndex} forward, recognising any
     * registered flags for {@code pathway}. Each recognised flag is
     * materialised into a {@link ParseResult} (using {@link #parseFlagArgument}
     * which delegates to the flag's input type for value flags), so the
     * execution-context drain receives a fully-parsed value with no need for
     * a second pass over raw input.
     */
    private TrailingFlags<S> consumeRemainingFlags(CommandPathway<S> pathway, RawInputStream<S> stream, int consumedIndex) {
        RawInputStream<S> remaining = stream.copyAndSetAtIndex(consumedIndex);
        Map<String, ParseResult<S>> results = null;
        int lastConsumed = consumedIndex;

        while (remaining.hasNext()) {
            String raw = remaining.next();
            if (!Patterns.isInputFlag(raw)) {
                break;
            }

            Set<FlagArgument<S>> extracted;
            try {
                extracted = pathway.getFlagExtractor().extract(raw);
            } catch (CommandException ignored) {
                break;
            }
            if (extracted.isEmpty()) {
                break;
            }

            int needed = studio.mevera.imperat.command.tree.FlagValueDrain.requiredTokenCount(extracted);
            List<String> valueTokens = studio.mevera.imperat.command.tree.FlagValueDrain.drain(remaining, needed);
            if (needed > 0 && valueTokens.isEmpty()) {
                // Value-flag(s) declared but the trailing input ran out of
                // tokens to satisfy them — discard the partial trailing-flag
                // region (matches legacy single-token "no value to bind"
                // semantic).
                return new TrailingFlags<>(consumedIndex, Collections.emptyMap());
            }

            if (results == null) {
                results = new LinkedHashMap<>();
            }
            for (FlagArgument<S> flag : extracted) {
                results.put(flag.getName(), parseFlagArgument(flag, raw, valueTokens, remaining));
            }
            lastConsumed = remaining.getRawIndex();
        }

        return new TrailingFlags<>(
                lastConsumed,
                results == null ? Collections.emptyMap() : results
        );
    }

    private ParseResult<S> parseFlagArgument(
            FlagArgument<S> flag,
            String rawFlagInput,
            @NotNull List<String> valueTokens,
            RawInputStream<S> inputStream
    ) {
        if (flag.isSwitch()) {
            return ParseResult.of(flag, rawFlagInput, true, null);
        }

        String joined = studio.mevera.imperat.command.tree.FlagValueDrain.join(valueTokens);
        if (valueTokens.isEmpty()) {
            return ParseResult.failedParse(flag, joined, null);
        }

        var inputType = flag.flagData().inputType();
        if (inputType == null) {
            return ParseResult.failedParse(flag, joined, new IllegalStateException("Missing input type for value flag"));
        }
        try {
            Object parsed = inputType.parse(
                    inputStream.getContext(),
                    flag,
                    studio.mevera.imperat.command.tree.FlagValueDrain.cursor(inputStream.getContext(), valueTokens)
            );
            return ParseResult.of(flag, joined, parsed, null);
        } catch (Throwable error) {
            return ParseResult.of(flag, joined, null, error);
        }
    }

    /**
     * Trailing-flag parse output. Carried separately from the main candidate
     * chain so that the failure-penalty in scoring (which is positional-arg
     * sensitive) does not unfairly penalise a candidate whose owner pathway
     * uniquely accepts the flag — error reporting for malformed flag values
     * is handled at drain time instead.
     */
    private record TrailingFlags<S extends CommandSource>(
            int consumedIndex,
            Map<String, ParseResult<S>> results
    ) {

    }
}
