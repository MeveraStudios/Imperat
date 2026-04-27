package studio.mevera.imperat.command.tree.walk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.command.tree.Node;
import studio.mevera.imperat.command.tree.ParseResult;
import studio.mevera.imperat.command.tree.ParsedNode;
import studio.mevera.imperat.command.tree.RawInputStream;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
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
 * Strategy producing tab-completion suggestions for a command tree, extracted
 * from the former {@code SuperCommandTree}. Walks the tree against the
 * already-typed prefix, picks the best surviving candidate, and emits the
 * appropriate next-token suggestions (argument provider output, flag names,
 * or flag-value suggestions).
 *
 * <p>The walk reuses the same {@link Candidate} + scoring machinery as
 * {@link TreeParser} to ensure suggestions track the exact branch a future
 * execution would resolve to.</p>
 */
public final class TreeSuggester<S extends CommandSource> {

    private final Node<S> root;
    private final ImperatConfig<S> imperatConfig;

    public TreeSuggester(@NotNull Node<S> root, @NotNull ImperatConfig<S> imperatConfig) {
        this.root = root;
        this.imperatConfig = imperatConfig;
    }

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
                                    .filter(c -> !WalkScoring.hasUnacceptable(c.chain()))
                                    .max(Comparator.comparingInt(c -> WalkScoring.scoreCandidate(c, totalTokens)))
                                    .orElse(null);
        if (best == null || best.chain().isEmpty()) {
            return Collections.emptyList();
        }
        if (isInsideSecretPath(best.chain())) {
            return Collections.emptyList();
        }

        ParsedNode<S> currentParsed = best.chain().get(best.chain().size() - 1);
        Node<S> currentNode = currentParsed.getDelegate();
        if (!hasSuggestionPermission(context, currentNode.getOriginalPathway())) {
            return Collections.emptyList();
        }

        FlagArgument<S> flagValueTarget = findFlagValueTarget(context, best.chain());
        if (flagValueTarget != null) {
            return filterByPrefix(collectFlagValueSuggestions(context, flagValueTarget), prefix);
        }

        List<String> suggestions = new ArrayList<>();
        addArgumentSuggestions(context, currentParsed, currentNode, suggestions);
        List<String> commandChain = commandChainFromParsedPath(best.chain());
        addFlagNameSuggestions(context, currentNode, commandChain, resolveUsedFlags(best.chain()), suggestions);
        return filterByPrefix(suggestions, prefix);
    }

    private boolean hasBlankGap(ArgumentInput fullInput, int completionIndex) {
        for (int i = 0; i < completionIndex && i < fullInput.size(); i++) {
            String token = fullInput.get(i);
            if (token != null && token.isBlank()) {
                return true;
            }
        }
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
            ParseResult<S> mainParse = parseArgument(node.getMainArgument(), inputStream);
            if (mainParse.isUnAcceptableScore()) {
                return null;
            }
            parseResultMap.put(node.getMainArgument().getName(), mainParse);
        }

        Iterator<? extends Argument<S>> optionalsIterator = node.getOptionalArguments().iterator();

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
            inputStream.backward();

            if (!optionalsIterator.hasNext()) {
                break;
            }
            Argument<S> optional = optionalsIterator.next();
            ParseResult<S> optionalParseResult = parseArgument(optional, inputStream);
            parseResultMap.put(optional.getName(), optionalParseResult);
        }

        return ParsedNode.of(node, parseResultMap);
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
            Object parsed = inputType.parse(
                    inputStream.getContext(),
                    flag,
                    Cursor.single(inputStream.getContext(), sharedValueInput)
            );
            return ParseResult.of(flag, sharedValueInput, parsed, null);
        } catch (Throwable error) {
            return ParseResult.of(flag, sharedValueInput, null, error);
        }
    }

    /**
     * Tab-completion variant: collect tokens up to the budget, hand the type a
     * Cursor over them, advance the stream by the type's actual consumption.
     * Mirrors the execution path's protocol in {@link Node} but keeps the
     * completion walk's relaxed "blank input → failure result rather than
     * throw" handling intact.
     */
    private ParseResult<S> parseArgument(Argument<S> argument, RawInputStream<S> inputStream) {
        int beforeIndex = inputStream.getRawIndex();
        List<String> tokens = collectTokensForCompletion(argument, inputStream);

        if (tokens.isEmpty()) {
            inputStream.setRawIndex(beforeIndex);
            if (argument.isCommand()) {
                return ParseResult.unacceptableParse(argument, "", new IndexOutOfBoundsException());
            }
            return ParseResult.failedParse(argument, "", null);
        }

        Cursor<S> rootCursor = Cursor.of(inputStream.getContext(), tokens);
        Cursor<S> probe = rootCursor.snapshot();

        try {
            var result = argument.type().parse(inputStream.getContext(), argument, probe);
            rootCursor.commitFrom(probe);
            int consumedTokenCount = probe.position();
            if (!isGreedyArgument(argument)) {
                inputStream.setRawIndex(beforeIndex + consumedTokenCount);
            }
            String consumedInput = rootCursor.slice(0, consumedTokenCount);
            return ParseResult.of(argument, consumedInput, result, null);
        } catch (Throwable error) {
            String attemptedInput = rootCursor.slice(0, tokens.size());
            if (argument.isCommand()) {
                return ParseResult.unacceptableParse(argument, attemptedInput, error);
            }
            return ParseResult.of(argument, attemptedInput, null, error);
        }
    }

    /**
     * Tab-completion budget collection. The completion walk doesn't apply
     * greedy-flag-aware extraction (that's an execution-only concern handled
     * in {@code Node#collectGreedyTokens}); here we treat greedy as "consume
     * up to limit" without flag awareness.
     */
    private List<String> collectTokensForCompletion(Argument<S> argument, RawInputStream<S> inputStream) {
        if (isGreedyArgument(argument)) {
            int limit = argument.greedyLimit();
            List<String> collected = new ArrayList<>();
            while (inputStream.hasNext()) {
                if (limit > 0 && collected.size() >= limit) {
                    break;
                }
                collected.add(inputStream.next());
            }
            return collected;
        }
        int toConsume = argument.type().getNumberOfParametersToConsume(argument);
        if (toConsume <= 0) {
            return List.of();
        }
        List<String> collected = new ArrayList<>(toConsume);
        while (inputStream.hasNext() && collected.size() < toConsume) {
            collected.add(inputStream.next());
        }
        return collected;
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
        for (Argument<S> optional : currentNode.getOptionalArguments()) {
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
        if (!isChildSuggestionVisible(context, child)) {
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

    private boolean isChildSuggestionVisible(SuggestionContext<S> context, Node<S> child) {
        Argument<S> main = child.getMainArgument();
        if (!main.isCommand()) {
            return isSuggestionVisible(context, main, child.getOriginalPathway());
        }

        if (main.asCommand().isSecret()) {
            return false;
        }

        if (context.command().isIgnoringACPerms()) {
            return true;
        }

        var checker = imperatConfig.getPermissionChecker();
        if (!checker.hasPermission(context.source(), main)) {
            return false;
        }

        return hasVisibleExecutablePathway(context, child);
    }

    private boolean hasVisibleExecutablePathway(SuggestionContext<S> context, Node<S> node) {
        var checker = imperatConfig.getPermissionChecker();
        boolean hasExecutableTerminal = false;
        for (CommandPathway<S> pathway : node.getTerminalPathways()) {
            if (pathway.getMethodElement() == null) {
                continue;
            }
            hasExecutableTerminal = true;
            if (checker.hasPermission(context.source(), pathway)) {
                return true;
            }
        }

        if (hasExecutableTerminal) {
            return false;
        }

        for (Node<S> child : node.getChildren()) {
            if (hasVisibleExecutablePathway(context, child)) {
                return true;
            }
        }

        return false;
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

    private boolean isGreedyArgument(Argument<S> argument) {
        return argument.isGreedy() || argument.type().isGreedy(argument);
    }
}
