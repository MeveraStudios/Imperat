package studio.mevera.imperat.command.parameters.type;

import static studio.mevera.imperat.util.StringUtils.isQuoteChar;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.UnknownFlagException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.Priority;

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
            throws UnknownFlagException {
        StringBuilder builder = new StringBuilder();

        final Character current = inputStream.currentLetter().orElse(null);
        if (current == null) {
            return input;
        }

        if (parameter != null && parameter.isGreedyString()) {
            handleGreedyOptimized(builder, inputStream, context);
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
     * Optimized greedy handling with better performance characteristics
     */
    private void handleGreedyOptimized(StringBuilder builder, Cursor<S> inputStream, ExecutionContext<S> context)
            throws UnknownFlagException {
        // If truly greedy (consumes multiple raw inputs), handle remaining
        while (inputStream.isCurrentRawInputAvailable()) {
            String nextRaw = inputStream.currentRaw().orElse(null);
            if (nextRaw != null) {

                if (Patterns.isInputFlag(nextRaw)) {
                    Set<FlagArgument<S>> extracted = context.getDetectedUsage().getFlagExtractor().extract(nextRaw);
                    if (!extracted.isEmpty()) {
                        inputStream.skipRaw();
                        if (extracted.stream().noneMatch(FlagArgument::isSwitch)) {
                            inputStream.skipRaw(); // Skip the value of the flag
                        }
                        continue;
                    }
                }

                builder.append(nextRaw);
                if (inputStream.peekRaw().isPresent()) {
                    builder.append(" ");
                }
                inputStream.skipRaw(); // Consume the raw input
            } else {
                break;
            }
        }
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return parameter.isGreedyString();
    }

    @Override
    public Priority priority() {
        return Priority.LOW;
    }
}