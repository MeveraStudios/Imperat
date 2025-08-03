package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;

import static studio.mevera.imperat.util.StringUtils.isQuoteChar;

public final class ParameterString<S extends Source> extends BaseParameterType<S, String> {
    
    ParameterString() {
        super();
    }
    
    @Override
    public @NotNull String resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> inputStream, @NotNull String input) throws ImperatException {
        final CommandParameter<S> parameter = inputStream.currentParameter().orElse(null);
        
        // OPTIMIZATION 1: Fast path for simple strings (90% of cases)
        if (canUseFastPath(parameter, input)) {
            return input;
        }
        
        // OPTIMIZATION 2: Only use letter precision when actually needed
        return resolveWithPrecision(inputStream, input, parameter);
    }
    
    /**
     * Determine if we can use the fast path (no letter-level processing needed)
     */
    private boolean canUseFastPath(CommandParameter<S> parameter, String input) {
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
    
    private String resolveWithPrecision(CommandInputStream<S> inputStream, String input, CommandParameter<S> parameter) {
        StringBuilder builder = new StringBuilder();
        
        final Character current = inputStream.currentLetter().orElse(null);
        if (current == null) {
            return input;
        }
        
        if (parameter != null && parameter.isGreedyString()) {
            handleGreedyOptimized(builder, inputStream, input);
            return builder.toString();
        }

        // Handle quoted strings - your original logic
        Character next;
        do {
            next = inputStream.popLetter().orElse(null);
            if (next == null) break;
            builder.append(next);
        } while (inputStream.hasNextRaw()
                && inputStream.peekLetter().map((ch) -> !isQuoteChar(ch))
                .orElse(false));
        
        return builder.toString();
    }
    
    /**
     * Optimized greedy handling with better performance characteristics
     */
    private void handleGreedyOptimized(StringBuilder builder, CommandInputStream<S> inputStream, String input) {
        // If truly greedy (consumes multiple raw inputs), handle remaining
        while (inputStream.hasNextRaw()) {
            String nextRaw = inputStream.currentRaw().orElse(null);
            if (nextRaw != null) {
                builder.append(nextRaw);
                System.out.println("APPENDING '" + nextRaw + "'");
                if(inputStream.peekRaw().isPresent()) {
                    builder.append(" ");
                }
                inputStream.skipRaw(); // Consume the raw input
            } else {
                break;
            }
        }
    }
}