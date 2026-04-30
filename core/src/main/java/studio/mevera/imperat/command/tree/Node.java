package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.Cursor;
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
public sealed class Node<S extends CommandSource> implements Prioritizable permits ParsedNode {

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
     * <p>Optional binding is strict positional: the first optional that
     * cannot type-parse the next token causes the loop to exit, leaving
     * remaining tokens for children/siblings. Skipped optionals get
     * defaulted by the execution-context drain. Middle-positioned optionals
     * are not supported — pathway construction enforces tail-only optionals.</p>
     */
    public void parseOptionalsAndFlags(RawInputStream<S> inputStream, Map<String, ParseResult<S>> parseResultMap) {
        int optionalCursor = 0;
        while (inputStream.hasNext()) {
            String peek = inputStream.next();
            if (Patterns.isInputFlag(peek)) {
                try {
                    var extracted = flags.extract(peek);
                    if (!extracted.isEmpty()) {
                        String inline = Patterns.inlineFlagValue(peek);
                        List<String> valueTokens;
                        if (inline != null) {
                            // `--name=value` / `-n=value` form: the value is
                            // already in the flag token, no stream drain needed.
                            valueTokens = List.of(inline);
                        } else {
                            int needed = FlagValueDrain.requiredTokenCount(extracted);
                            valueTokens = FlagValueDrain.drain(inputStream, needed);
                        }
                        for (var extractedFlag : extracted) {
                            parseResultMap.put(
                                    extractedFlag.getName(),
                                    this.parseFlagArgument(extractedFlag, peek, valueTokens, inputStream)
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

            if (optionalCursor >= optionals.size()) {
                // No optional left to bind: hand the token back to children/siblings.
                break;
            }

            int beforeProbe = inputStream.getRawIndex();
            Argument<S> head = optionals.get(optionalCursor);
            ParseResult<S> headResult = this.parseArgument(head, inputStream);

            if (!headResult.isFailureScore() || head.isCommand()) {
                parseResultMap.put(head.getName(), headResult);
                optionalCursor++;
                continue;
            }

            // Head failed type-parse — strict positional binding leaves the
            // token for children/siblings.
            inputStream.setRawIndex(beforeProbe);
            break;
        }
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

        String joined = FlagValueDrain.join(valueTokens);
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
                    FlagValueDrain.cursor(inputStream.getContext(), valueTokens)
            );
            return ParseResult.of(flag, joined, parsed, null);
        } catch (Throwable error) {
            return ParseResult.of(flag, joined, null, error);
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
    /**
     * Hands the argument's type a {@link Cursor} over the tokens the tree has
     * allocated to it, then commits the cursor's actual consumption back to
     * {@code inputStream}.
     *
     * <p>Three-tier token budget dispatch by argument shape:
     * <ul>
     *   <li><b>Tier 1 — Simple ({@link SimpleArgumentType}).</b> Fixed token
     *       count from {@link ArgumentType#getNumberOfParametersToConsume}.
     *       Pre-collected via {@link #collectFixedTokens}.</li>
     *   <li><b>Tier 2 — Greedy.</b> {@link #collectGreedyTokens} applies
     *       reservation, type-discrimination yield, and inline flag
     *       extraction. Type expected to consume the full budget.</li>
     *   <li><b>Tier 3 — Complex.</b> Any custom {@link ArgumentType} that is
     *       neither Simple nor Greedy. The framework drains all remaining
     *       tokens into the budget via {@link #collectComplexTokens} and lets
     *       the type's {@link ArgumentType#parse} consume an
     *       implementation-defined prefix; the cursor's post-parse position
     *       is the authoritative consumption count.</li>
     * </ul></p>
     *
     * <p>Rollback policy:
     * <ul>
     *   <li>On a thrown {@link Throwable}: roll {@code inputStream} fully back
     *       to its position before this method ran.</li>
     *   <li>On success for Tier 1 / Tier 3: roll {@code inputStream} forward
     *       by exactly {@code cursor.position()} tokens, returning the unused
     *       budget suffix to the stream for downstream arguments.</li>
     *   <li>On success for Tier 2 (greedy): leave {@code inputStream} where
     *       greedy pre-collection placed it — drain semantics imply the type
     *       owned the full budget.</li>
     * </ul></p>
     */
    private ParseResult<S> parseArgument(
            Argument<S> argument,
            RawInputStream<S> inputStream,
            @Nullable Map<String, ParseResult<S>> flagSink
    ) {
        int beforeIndex = inputStream.getRawIndex();
        boolean greedy = isGreedyArgument(argument);
        boolean complex = !greedy && isComplexArgument(argument);
        List<String> tokens;
        if (greedy) {
            tokens = collectGreedyTokens(argument, inputStream, flagSink);
        } else if (complex) {
            tokens = collectComplexTokens(inputStream);
        } else {
            tokens = collectFixedTokens(argument, inputStream);
        }

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
            var value = argument.type().parse(inputStream.getContext(), argument, probe);
            rootCursor.commitFrom(probe);

            int consumedTokenCount = probe.position();
            if (!greedy) {
                // Tier 1 / Tier 3: roll inputStream forward by exactly the
                // tokens the type consumed; unused budget tokens flow back to
                // the stream for the next argument. (Greedy / Tier 2 keeps
                // its pre-collected position — drain semantics.)
                inputStream.setRawIndex(beforeIndex + consumedTokenCount);
            }

            String consumedInput = rootCursor.slice(0, consumedTokenCount);
            return ParseResult.of(argument, consumedInput, value, null);
        } catch (Throwable error) {
            // Don't roll inputStream back — the budget tokens *were* allocated
            // to this argument, the type just failed to parse them. Mirrors
            // the legacy behaviour where a failed parse still committed the
            // pre-joined input span; the candidate is admitted with a failure
            // ParseResult and the drain re-throws the typed error so users
            // can register exception handlers for it.
            String attemptedInput = rootCursor.slice(0, tokens.size());
            if (argument.isCommand()) {
                return ParseResult.unacceptableParse(argument, attemptedInput, error);
            }
            return ParseResult.of(argument, attemptedInput, null, error);
        }
    }

    /**
     * Greedy budget collection. Pulls tokens from {@code inputStream} subject
     * to:
     * <ul>
     *   <li>The argument's {@code @Greedy(limit = N)} cap.</li>
     *   <li>Reservation for downstream required arguments when the next one
     *       cannot discriminate by type (string-shaped).</li>
     *   <li>Type-discrimination yield when the next arg is a non-String type
     *       that accepts a token.</li>
     *   <li>Inline registered flag extraction into {@code flagSink} (when
     *       non-null). Flag tokens advance the stream but are not added to the
     *       greedy budget.</li>
     * </ul>
     */
    List<String> collectGreedyTokens(
            Argument<S> argument,
            RawInputStream<S> inputStream,
            @Nullable Map<String, ParseResult<S>> flagSink
    ) {
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

        List<String> collected = new ArrayList<>();
        while (inputStream.hasNext()) {
            if (effectiveLimit > 0 && collected.size() >= effectiveLimit) {
                break;
            }
            String peek = inputStream.next();
            if (flagSink != null && Patterns.isInputFlag(peek)
                        && tryExtractInlineFlag(peek, inputStream, flagSink)) {
                continue;
            }
            if (!collected.isEmpty()
                        && nextCanDiscriminate
                        && matchesParameterType(inputStream.getContext(), nextDownstream, peek)) {
                inputStream.backward();
                break;
            }
            collected.add(peek);
        }
        return collected;
    }

    /**
     * Fixed-arity budget collection. Pulls up to
     * {@link ArgumentType#getNumberOfParametersToConsume} tokens from the
     * stream. Negative or zero declared counts collect nothing.
     */
    private List<String> collectFixedTokens(Argument<S> argument, RawInputStream<S> inputStream) {
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

    /**
     * Tier-3 (complex / variable-arity) budget collection. Drains every
     * remaining token from the stream into the budget. The argument type's
     * {@link ArgumentType#parse} consumes an implementation-defined prefix
     * via the cursor; {@link #parseArgument} rolls the unused suffix back
     * to {@code inputStream} based on {@code cursor.position()} after parse.
     */
    private List<String> collectComplexTokens(RawInputStream<S> inputStream) {
        if (!inputStream.hasNext()) {
            return List.of();
        }
        List<String> collected = new ArrayList<>();
        while (inputStream.hasNext()) {
            collected.add(inputStream.next());
        }
        return collected;
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
            String inline = Patterns.inlineFlagValue(rawFlagInput);
            List<String> valueTokens;
            if (inline != null) {
                valueTokens = List.of(inline);
            } else {
                int needed = FlagValueDrain.requiredTokenCount(extracted);
                valueTokens = FlagValueDrain.drain(inputStream, needed);
            }
            for (var extractedFlag : extracted) {
                flagSink.put(
                        extractedFlag.getName(),
                        this.parseFlagArgument(extractedFlag, rawFlagInput, valueTokens, inputStream)
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
     * Tier-3 detector: an {@link ArgumentType} that is neither
     * {@link SimpleArgumentType} nor greedy — a custom variable-arity type
     * whose token consumption is decided at parse time by the cursor's
     * post-parse position. {@link #parseArgument} drains all remaining
     * stream tokens into the budget for these.
     */
    private boolean isComplexArgument(Argument<S> argument) {
        return !(argument.type() instanceof studio.mevera.imperat.command.arguments.type.SimpleArgumentType<?, ?>);
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
            argument.type().parse(context, argument, Cursor.single(context, input));
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
