package studio.mevera.imperat.command.arguments.type;

import static studio.mevera.imperat.util.StringUtils.isQuoteChar;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.priority.Priority;

import java.util.Set;

public final class StringArgument<S extends Source> extends ArgumentType<S, String> {

    StringArgument() {
        super();
    }

    @Override
    public @NotNull String parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor, @NotNull String correspondingInput) throws
            CommandException {
        final Argument<S> parameter = cursor.currentParameter().orElse(null);

        // OPTIMIZATION 1: Fast path for simple strings (90% of cases)
        if (canUseFastPath(parameter, correspondingInput)) {
            return correspondingInput;
        }

        // OPTIMIZATION 2: Only use letter precision when actually needed
        return resolveWithPrecision(context, cursor, correspondingInput, parameter);
    }

    /**
     * Determine if we can use the fast path (no letter-level processing needed)
     */
    private boolean canUseFastPath(Argument<S> parameter, String input) {
        // Fast path conditions:
        // 1. Not a greedy string (doesn't need to consume multiple inputs)
        // 2. Doesn't start with quotes (no quote parsing needed)
        // 3. Input is not null/empty

        if (input == null || input.isEmpty()) {
            return false;
        }

        if (parameter != null && parameter.isGreedyString()) {
            return false; // Greedy strings need special handling
        }

        return !isQuoteChar(input.charAt(0)); // Quoted strings need letter-level parsing
    }

    private String resolveWithPrecision(ExecutionContext<S> context, Cursor<S> inputStream, String input, Argument<S> parameter)
            throws CommandException {
        StringBuilder builder = new StringBuilder();

        final Character current = inputStream.currentLetter().orElse(null);
        if (current == null) {
            return input;
        }

        if (parameter != null && parameter.isGreedyString()) {
            int limit = -1; // Default: unlimited
            if (parameter.isAnnotated()) {
                Greedy greedyAnn = parameter.asAnnotatedArgument().getAnnotation(Greedy.class);
                if (greedyAnn == null) {
                    throw new CommandException("Greedy annotation missing on an annotated parameter marked as greedy string, there's something "
                                                       + "wrong!");
                }
                limit = greedyAnn.limit();
            }
            handleGreedyOptimized(builder, limit, inputStream, context);
            return builder.toString();
        }

        // Handle quoted strings - your original logic
        Character next;
        do {
            next = inputStream.popLetter().orElse(null);
            if (next == null) {
                break;
            }
            builder.append(next);
        } while (inputStream.isCurrentRawInputAvailable()
                         && inputStream.peekLetter().map((ch) -> !isQuoteChar(ch))
                                    .orElse(false));

        return builder.toString();
    }

    /**
     * Greedy consumption with smart yielding.
     * <p>
     * Consumes tokens greedily up to {@code limit} (or all remaining if unlimited),
     * but stops early if:
     * <ol>
     *   <li>The next raw token is a flag ({@code isInputFlag})</li>
     *   <li>The next parameter has a discriminating type (not String) AND the next
     *       raw token matches that type — yield it to the next parameter</li>
     *   <li>If the next parameter is also String (no type discrimination possible),
     *       reserve tokens for all trailing required parameters</li>
     * </ol>
     */
    private void handleGreedyOptimized(StringBuilder builder, int limit, Cursor<S> inputStream, ExecutionContext<S> context) throws CommandException {
        // Find the next non-flag parameter after this one (if any)
        Argument<S> nextParam = GreedyLimitHelper.findNextNonFlagParam(inputStream);
        boolean nextParamCanDiscriminate = nextParam != null && !nextParam.isGreedyString()
                                                   && !(nextParam.type() instanceof StringArgument<?>);

        int effectiveLimit = GreedyLimitHelper.computeEffectiveLimit(
                limit, nextParam, nextParamCanDiscriminate, inputStream
        );

        int consumed = 0;

        // Skip any leading flag tokens that appear before the actual greedy content.
        // e.g. "motd -time 1h Hello world" — the cursor starts at "-time", which must be skipped.
        while (inputStream.isCurrentRawInputAvailable()) {
            String candidate = inputStream.currentRaw().orElse(null);
            if (candidate == null) {
                return;
            }
            if (Patterns.isInputFlag(candidate)) {
                Set<FlagArgument<S>> extracted = context.getDetectedPathway().getFlagExtractor().extract(candidate);
                if (!extracted.isEmpty()) {
                    inputStream.skipRaw(); // skip the flag name
                    if (extracted.stream().noneMatch(FlagArgument::isSwitch)) {
                        inputStream.skipRaw(); // skip the flag value
                    }
                    continue;
                }
            }
            break; // not a flag — this is the real first raw
        }

        // Append the FIRST real raw token.
        String firstRaw = inputStream.currentRaw().orElse(null);
        if (firstRaw == null) {
            return;
        }
        builder.append(firstRaw);
        consumed++;

        // Consume subsequent raws
        while (inputStream.hasNextRaw()) {
            // Hard cap: if effective limit is set and we've reached it, stop
            if (effectiveLimit > 0 && consumed >= effectiveLimit) {
                break;
            }

            // Peek at the next raw WITHOUT consuming it yet
            String peeked = inputStream.peekRawIfPresent();
            if (peeked == null) {
                break;
            }

            // Stop condition 1: next raw is a flag → end greedy
            if (Patterns.isInputFlag(peeked)) {
                Set<FlagArgument<S>> extracted = context.getDetectedPathway().getFlagExtractor().extract(peeked);
                if (!extracted.isEmpty()) {
                    // Skip the flag tokens (they'll be handled by ParameterChain)
                    inputStream.skipRaw(); // advance past current to the flag
                    inputStream.skipRaw(); // skip the flag name
                    if (extracted.stream().noneMatch(FlagArgument::isSwitch)) {
                        inputStream.skipRaw(); // skip the flag value
                    }
                    continue;
                }
            }

            // Stop condition 2: next param has a discriminating type and the
            // peeked token matches it — yield the token to the next parameter
            if (nextParamCanDiscriminate) {
                int peekRawPos = inputStream.currentRawPosition() + 1;
                if (nextParam.type().matchesInput(peekRawPos, context, nextParam)) {
                    break;
                }
            }

            // Consume the token
            inputStream.skipRaw();
            builder.append(" ").append(peeked);
            consumed++;
        }
        // Cursor is left pointing at the LAST consumed raw.
        // The caller's `stream.skip()` will advance past it.
    }


    @Override
    public boolean isGreedy(Argument<S> parameter) {
        // A limited greedy is still greedy from the tree's perspective —
        // the cap is enforced during parsing, not during tree dispatch.
        return parameter.isGreedyString();
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<S> argument) {
        // Always report 1 — the greedy cursor consumes the rest internally.
        // Returning the limit here would cause the tree to advance N depths
        // and reject inputs with fewer than N tokens.
        return 1;
    }

    @Override
    public Priority priority() {
        return Priority.LOW;
    }
}