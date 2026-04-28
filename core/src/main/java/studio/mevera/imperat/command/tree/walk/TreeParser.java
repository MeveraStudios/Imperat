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
 * from the former {@code SuperCommandTree} with no behavioural changes:
 * traversal, scoring, candidate admission, and trailing-flag handling are all
 * preserved verbatim.
 */
public final class TreeParser<S extends CommandSource> {

    private final Node<S> root;

    public TreeParser(@NotNull Node<S> root) {
        this.root = root;
    }

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

    private void traverse(
            Node<S> node,
            RawInputStream<S> stream,
            List<ParsedNode<S>> path,
            List<Candidate<S>> candidates
    ) throws CommandException {

        if (node.isRoot()) {
            int rootSaved = stream.getRawIndex();
            ParsedNode<S> rootParsed = ParsedNode.of(node, new HashMap<>());
            path.add(rootParsed);
            addExecutionCandidates(node, stream, path, candidates, rootSaved, false);
            if (!node.isLeaf() && stream.hasNext()) {
                for (Node<S> child : node.getChildren()) {
                    int saved = stream.getRawIndex();
                    traverse(child, stream, path, candidates);
                    stream.setRawIndex(saved);
                }
            }
            path.remove(path.size() - 1);

            stream.setRawIndex(rootSaved);
            Map<String, ParseResult<S>> rootResults = new HashMap<>();
            node.parseOptionalsAndFlags(stream, rootResults);
            int afterRootParse = stream.getRawIndex();
            if (!rootResults.isEmpty() || afterRootParse != rootSaved) {
                ParsedNode<S> rootWithOptionals = ParsedNode.of(node, rootResults);
                path.add(rootWithOptionals);
                if (node.isLeaf() || hasExecutableTerminal(node)) {
                    addExecutionCandidates(node, stream, path, candidates, afterRootParse, node.isLeaf());
                }
                if (!node.isLeaf() && stream.hasNext()) {
                    for (Node<S> child : node.getChildren()) {
                        int saved = stream.getRawIndex();
                        traverse(child, stream, path, candidates);
                        stream.setRawIndex(saved);
                    }
                }
                path.remove(path.size() - 1);
            }
            stream.setRawIndex(rootSaved);
            return;
        }

        int saved = stream.getRawIndex();
        ParsedNode<S> parsed;
        try {
            parsed = node.parseArgument(stream);
        } catch (Throwable t) {
            stream.setRawIndex(saved);
            return;
        }
        if (parsed == null) {
            stream.setRawIndex(saved);
            return;
        }

        path.add(parsed);
        int afterParse = stream.getRawIndex();

        if (node.isLeaf()) {
            addExecutionCandidates(node, stream, path, candidates, afterParse, allowsLeafPartialCandidate(node));
        } else if (!stream.hasNext()) {
            addExecutionCandidates(node, stream, path, candidates, afterParse, true);
        } else {
            for (Node<S> child : node.getChildren()) {
                int childSaved = stream.getRawIndex();
                traverse(child, stream, path, candidates);
                stream.setRawIndex(childSaved);
            }
        }

        path.remove(path.size() - 1);
        stream.setRawIndex(saved);
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
