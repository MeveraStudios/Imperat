package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.flags.FlagExtractor;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.priority.Prioritizable;
import studio.mevera.imperat.util.priority.Priority;
import studio.mevera.imperat.util.priority.PriorityList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A node is made of the MAIN REQUIRED argument its based on, additional optional args afterward, and flags
 * INSTEAD OF HAVING a node for every argument.
 * @param <S> the command source
 */
public class Node<S extends CommandSource> implements Prioritizable {

    /*
     * The required argument this node is based on
     */
    final Argument<S> main;

    /**
     * List of optional arguments within this node
     */
    final List<Argument<S>> optionals;

    /**
     * The map of flags for this node.
     */
    final FlagExtractor<S> flags;
    final PriorityList<Node<S>> children = new PriorityList<>();
    final List<CommandPathway<S>> terminalPathways = new ArrayList<>();
    final @NotNull CommandPathway<S> originalPathway;
    @Nullable Node<S> parent;

    Node(@Nullable Node<S> parent, @NotNull CommandPathway<S> originalPathway, Argument<S> main, List<Argument<S>> optionals) {
        this.parent = parent;
        this.originalPathway = originalPathway;
        this.main = main;
        this.optionals = optionals;
        this.flags = originalPathway.getFlagExtractor();
    }

    Node(@Nullable Node<S> parent, @NotNull CommandPathway<S> originalPathway, Argument<S> main) {
        this(parent, originalPathway, main, new ArrayList<>());
    }

    public @Nullable Node<S> getParent() {
        return parent;
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public @NotNull CommandPathway<S> getOriginalPathway() {
        return originalPathway;
    }

    public @NotNull List<CommandPathway<S>> getTerminalPathways() {
        return List.copyOf(terminalPathways);
    }

    void addTerminalPathway(@NotNull CommandPathway<S> pathway) {
        for (CommandPathway<S> existing : terminalPathways) {
            if (existing == pathway) {
                return;
            }
        }
        terminalPathways.add(pathway);
    }

    public boolean isExecutable() {
        return !terminalPathways.isEmpty();
    }

    /**
     * Parses this node against the given input stream. Advances the stream
     * by the number of tokens consumed by this node (main + optionals + flags).
     * Caller is responsible for snapshotting the stream (via {@link RawInputStream#copy()}
     * or {@link RawInputStream#getRawIndex()}) before calling if backtracking is needed.
     *
     * @return a {@link ParsedNode} wrapping this node + per-argument {@link ParseResult}s,
     *         or {@code null} if this node's main argument failed with an UNACCEPTABLE score.
     */
    @Nullable
    public ParsedNode<S> parseArgument(RawInputStream<S> inputStream) throws CommandException {
        Map<String, ParseResult<S>> parseResultMap = new HashMap<>();

        // Threaded {@code parseResultMap} so that registered flags appearing
        // inside a greedy main argument's span are extracted into the result
        // map instead of being slurped into the greedy buffer. Non-greedy mains
        // consume a fixed token count and never see flag tokens.
        ParseResult<S> mainParsedResult = this.parseArgument(main, inputStream, parseResultMap);
        if (mainParsedResult.isUnAcceptableScore()) {
            return null;
        }
        parseResultMap.put(main.getName(), mainParsedResult);

        parseOptionalsAndFlags(inputStream, parseResultMap);
        return new ParsedNode<>(this, parseResultMap);
    }

    /**
     * Consumes the trailing optionals and inline flags this node owns. Stops as soon
     * as a non-flag token has no remaining optional to bind to (so children can
     * still pick it up). Used by {@link #parseArgument(RawInputStream)} for normal
     * nodes and by the tree directly for the synthetic root traversal.
     *
     * <p>When {@link studio.mevera.imperat.ImperatConfig#handleExecutionMiddleOptionalSkipping()
     * smart optional skipping} is enabled and the next optional cannot accept the
     * current token, this method probes subsequent optionals (in declaration order)
     * for one that does. Skipped optionals are simply omitted from
     * {@code parseResultMap} — the execution-context drain materialises their
     * declared defaults later, so the tree remains the single source of truth for
     * the chain and the drain stays a pure projection.</p>
     */
    public void parseOptionalsAndFlags(RawInputStream<S> inputStream, Map<String, ParseResult<S>> parseResultMap) {
        boolean smartSkippingEnabled = inputStream.getContext()
                                               .imperatConfig()
                                               .handleExecutionMiddleOptionalSkipping();

        int optionalCursor = 0;
        while (inputStream.hasNext()) {
            String peek = inputStream.next();
            if (Patterns.isInputFlag(peek)) {
                try {
                    var extracted = flags.extract(peek);
                    if (!extracted.isEmpty()) {
                        boolean hasValueFlags = extracted.stream().anyMatch(flag -> !flag.isSwitch());
                        String sharedValueInput = hasValueFlags && inputStream.hasNext() ? inputStream.next() : null;
                        for (var extractedFlag : extracted) {
                            parseResultMap.put(
                                    extractedFlag.getName(),
                                    this.parseFlagArgument(extractedFlag, peek, sharedValueInput, inputStream)
                            );
                        }
                        continue;
                    }
                } catch (CommandException ignored) {
                    // Not a registered flag for this pathway: treat as normal token.
                }
                inputStream.backward();
            } else {
                inputStream.backward();
            }

            // Obligation skipping: if binding the next optional would leave too
            // few tokens for downstream required arguments, skip it (the drain
            // materialises its declared default) and try the optional after it.
            // Mirrors {@code OptionalArgumentHandler#calculateObligationToSkip}.
            while (optionalCursor < optionals.size()
                           && mustSkipForDownstream(inputStream)) {
                optionalCursor++;
            }

            if (optionalCursor >= optionals.size()) {
                // No optional left to bind: hand the token back to children/siblings.
                break;
            }

            int beforeProbe = inputStream.getRawIndex();
            Argument<S> head = optionals.get(optionalCursor);
            ParseResult<S> headResult = this.parseArgument(head, inputStream);

            if (!headResult.isFailureScore() || head.isCommand()) {
                // Clean bind: keep the result and advance to the next optional slot.
                parseResultMap.put(head.getName(), headResult);
                optionalCursor++;
                continue;
            }

            // Head failed type-parse. Roll the cursor back so probes start fresh
            // on the same token we just rejected.
            inputStream.setRawIndex(beforeProbe);

            if (!smartSkippingEnabled) {
                // Strict positional binding (config-disabled smart skipping):
                // leave the token for children/siblings, matching the legacy
                // linear "break on failed optional" semantics.
                break;
            }

            // Smart skipping: probe subsequent optionals in declaration order.
            // The first one whose type accepts the token wins; intervening
            // optionals are silently skipped and defaulted by the drain. This
            // replaces OptionalArgumentHandler#findBestDownstreamMatch but
            // performed during the tree walk so the resulting ParsedNode is
            // canonical.
            Argument<S> matched = null;
            ParseResult<S> matchedResult = null;
            int matchedIndex = -1;
            for (int probe = optionalCursor + 1; probe < optionals.size(); probe++) {
                Argument<S> candidate = optionals.get(probe);
                ParseResult<S> candidateResult = this.parseArgument(candidate, inputStream);
                if (!candidateResult.isFailureScore() || candidate.isCommand()) {
                    matched = candidate;
                    matchedResult = candidateResult;
                    matchedIndex = probe;
                    break;
                }
                inputStream.setRawIndex(beforeProbe);
            }

            if (matched == null) {
                // No downstream optional accepts this token either — leave it
                // for children/siblings, exactly as the legacy linear loop did.
                inputStream.setRawIndex(beforeProbe);
                break;
            }

            parseResultMap.put(matched.getName(), matchedResult);
            optionalCursor = matchedIndex + 1;
        }
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
        return parseArgument(argument, inputStream, null);
    }

    /**
     * Single-argument parse helper.
     *
     * <p>When {@code argument} is greedy and {@code flagSink} is non-null,
     * registered flags encountered inside the greedy span are extracted into
     * {@code flagSink} instead of being appended to the greedy buffer. This is
     * how the tree preserves the legacy "greedy slurps everything except
     * registered flags" semantics that used to be re-derived by the second-pass
     * {@code resolveInputFlags} in the execution-context drain.</p>
     *
     * <p>{@code flagSink} is {@code null} when this is a probe parse (used by
     * {@link #parseOptionalsAndFlags} to test downstream optionals); probes
     * must not produce side effects.</p>
     */
    private ParseResult<S> parseArgument(
            Argument<S> argument,
            RawInputStream<S> inputStream,
            @Nullable Map<String, ParseResult<S>> flagSink
    ) {
        StringBuilder builder = new StringBuilder();

        if (isGreedyArgument(argument)) {
            // Greedy collection has to honour two constraints inherited from
            // the legacy ArgumentValueBinder.collectGreedyInput logic, otherwise
            // a greedy main slurps tokens that downstream required arguments
            // need:
            //
            //  1. RESERVATION — when the next downstream required argument is
            //     also string-shaped (cannot discriminate by type), reserve
            //     enough tokens for it (and any further required args). The
            //     greedy limit is clamped to {@code available - reserve}.
            //  2. TYPE-DISCRIMINATION YIELD — when the next downstream required
            //     argument has a discriminating type (non-String), the greedy
            //     stops as soon as it sees a token that the downstream type
            //     accepts, leaving that token for the downstream parse.
            //
            // The "first token always wins" rule is preserved: discrimination
            // and limit checks only fire once we already have at least one
            // token in the buffer.
            int limit = argument.greedyLimit();
            Argument<S> nextDownstream = nextDiscriminatingArgument();
            boolean nextCanDiscriminate = canDiscriminate(nextDownstream);
            int effectiveLimit = limit;
            if (nextDownstream != null && !nextCanDiscriminate) {
                int reserve = countReservedRawsForDownstream();
                int rawsAvailable = inputStream.size() - (inputStream.getRawIndex() + 1);
                int maxByReserve = rawsAvailable - reserve;
                effectiveLimit = limit > 0 ? Math.min(limit, maxByReserve) : maxByReserve;
                effectiveLimit = Math.max(effectiveLimit, 1);
            }

            int consumed = 0;
            while (inputStream.hasNext()) {
                if (effectiveLimit > 0 && consumed >= effectiveLimit) {
                    break;
                }
                String peek = inputStream.next();
                if (flagSink != null && Patterns.isInputFlag(peek)
                            && tryExtractInlineFlag(peek, inputStream, flagSink)) {
                    // The token was a registered flag for this pathway; it has
                    // been bound into {@code flagSink} (along with any value
                    // token for value flags). Don't add it to the greedy buffer.
                    continue;
                }
                if (consumed > 0
                            && nextCanDiscriminate
                            && matchesParameterType(inputStream.getContext(), nextDownstream, peek)) {
                    // Hand the token back to the downstream parse.
                    inputStream.backward();
                    break;
                }
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(peek);
                consumed++;
            }
        } else {
            final int toConsume = argument.type().getNumberOfParametersToConsume(argument);
            int consumed = 0;
            while (inputStream.hasNext() && consumed < toConsume) {
                if (!builder.isEmpty()) {
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

    /**
     * Attempts to bind {@code rawFlagInput} as one or more registered flags
     * for this node's pathway, consuming a value token from {@code inputStream}
     * if any of the matched flags is a value flag (not a switch).
     *
     * @return {@code true} if at least one registered flag was matched and
     *         bound into {@code flagSink}; {@code false} if the token does not
     *         match any registered flag (caller treats it as ordinary input).
     */
    private boolean tryExtractInlineFlag(
            String rawFlagInput,
            RawInputStream<S> inputStream,
            Map<String, ParseResult<S>> flagSink
    ) {
        try {
            var extracted = flags.extract(rawFlagInput);
            if (extracted.isEmpty()) {
                return false;
            }
            boolean hasValueFlags = extracted.stream().anyMatch(flag -> !flag.isSwitch());
            String sharedValueInput = hasValueFlags && inputStream.hasNext() ? inputStream.next() : null;
            for (var extractedFlag : extracted) {
                flagSink.put(
                        extractedFlag.getName(),
                        this.parseFlagArgument(extractedFlag, rawFlagInput, sharedValueInput, inputStream)
                );
            }
            return true;
        } catch (CommandException ignored) {
            return false;
        }
    }

    public Argument<S> getMainArgument() {
        return main;
    }

    public Argument<S> getData() {
        return main;
    }

    public @NotNull Collection<? extends Argument<S>> getOptionalArguments() {
        return optionals;
    }

    public String format() {
        return main.format();
    }

    public boolean isGreedy() {
        return isGreedyArgument(getMainArgument());
    }

    private boolean isGreedyArgument(Argument<S> argument) {
        return argument.isGreedy() || argument.type().isGreedy(argument);
    }

    /**
     * Returns the next non-flag main argument reachable by descending into the
     * first child of each subsequent node, or {@code null} if no such argument
     * exists. Used by greedy-collection logic to decide reservation and type
     * discrimination for downstream required arguments.
     */
    @Nullable
    private Argument<S> nextDiscriminatingArgument() {
        Node<S> n = this;
        while (!n.children.isEmpty()) {
            Node<S> first = n.children.iterator().next();
            Argument<S> arg = first.main;
            if (arg.isFlag()) {
                n = first;
                continue;
            }
            return arg;
        }
        return null;
    }

    /**
     * Returns {@code true} if binding the next available token to an optional
     * at this node would leave too few raws for the downstream required
     * arguments. The caller should skip the optional (the drain will materialise
     * its declared default) and either try the next optional or hand the token
     * back to children. Mirrors the legacy
     * {@code OptionalArgumentHandler#calculateObligationToSkip}.
     *
     * <p>Reservation does not apply when this node already terminates a
     * <em>user-defined</em> pathway (see {@link #hasUserExecutableTerminal()}):
     * in that case the user can stop the command at this node, so downstream
     * descent is one alternative rather than mandatory. The carve-out
     * intentionally ignores synthetic empty-default terminals attached to the
     * root by the tree builder — those exist for every command and would
     * otherwise disable obligation skipping everywhere.</p>
     */
    private boolean mustSkipForDownstream(RawInputStream<S> stream) {
        if (hasUserExecutableTerminal()) {
            return false;
        }
        int rawsAvailable = stream.size() - (stream.getRawIndex() + 1);
        int reserve = countReservedRawsForDownstream();
        return rawsAvailable <= reserve;
    }

    /**
     * Whether any of this node's terminal pathways was registered with a
     * concrete user method (as opposed to the synthetic empty default attached
     * by the tree builder). Mirrors {@code SuperCommandTree#hasExecutableTerminal}.
     */
    private boolean hasUserExecutableTerminal() {
        for (CommandPathway<S> pathway : terminalPathways) {
            if (pathway.getMethodElement() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the raw tokens that need to be reserved for downstream required
     * arguments — one token per node depth on the canonical descent path.
     * Optional arguments and flags don't reserve.
     */
    private int countReservedRawsForDownstream() {
        int reserve = 0;
        Node<S> n = this;
        while (!n.children.isEmpty()) {
            Node<S> first = n.children.iterator().next();
            if (!first.main.isFlag()) {
                reserve++;
            }
            n = first;
        }
        return reserve;
    }

    /**
     * Whether {@code argument}'s type is concrete enough to recognise its own
     * inputs (i.e. not a {@link String}-shaped greedy fallback). When this is
     * true the greedy collector uses {@link #matchesParameterType} to yield
     * tokens to {@code argument} as soon as they parse cleanly.
     */
    private boolean canDiscriminate(@Nullable Argument<S> argument) {
        if (argument == null || argument.isGreedyString()) {
            return false;
        }
        return !TypeWrap.of(String.class).isSupertypeOf(argument.valueType());
    }

    /**
     * Pure type-match probe: returns {@code true} iff {@code argument}'s
     * argument type accepts {@code input} without error. Used purely for
     * yield decisions; the actual binding happens later via the canonical
     * tree walk.
     */
    private boolean matchesParameterType(CommandContext<S> context, @Nullable Argument<S> argument, String input) {
        if (argument == null) {
            return false;
        }
        try {
            argument.type().parse(context, argument, input);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public PriorityList<Node<S>> getChildren() {
        return children;
    }

    @Override
    public @NotNull Priority getPriority() {
        return main.type().getPriority();
    }
}
