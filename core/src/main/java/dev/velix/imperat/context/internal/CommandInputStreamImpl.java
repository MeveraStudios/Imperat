package dev.velix.imperat.context.internal;

import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.context.ArgumentQueue;
import dev.velix.imperat.context.Source;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Optional;

final class CommandInputStreamImpl<S extends Source> implements CommandInputStream<S> {
    
    private final String inputLine;
    private final StreamPosition<S> streamPosition;
    private final ArgumentQueue queue;
    private final List<CommandParameter<S>> parametersList;
    
    // Cache to store the starting position of each raw argument in the input line
    private final int[] rawStartPositions;
    
    // Performance optimization: Cache frequently accessed values
    private CommandParameter<S> cachedCurrentParameter = null;
    private String cachedCurrentRaw = null;
    private int lastParameterPosition = -1;
    private int lastRawPosition = -1;
    private boolean cacheValid = false;
    
    CommandInputStreamImpl(ArgumentQueue queue, CommandUsage<S> parametersList) {
        this(queue, parametersList.getParameters());
    }
    
    CommandInputStreamImpl(ArgumentQueue queue, List<CommandParameter<S>> parameters) {
        this(queue, parameters, new StreamPosition<>(parameters.size(), queue.size()), calculateRawStartPositions(queue, queue.getOriginalRaw()));
    }
    
    CommandInputStreamImpl(ArgumentQueue queue, List<CommandParameter<S>> parameters, StreamPosition<S> streamPosition, int[] rawStartPositions) {
        this.queue = queue;
        this.inputLine = queue.getOriginalRaw();
        this.parametersList = parameters;
        this.streamPosition = streamPosition;
        this.rawStartPositions = rawStartPositions;
        updateCache(); // Initialize cache
    }
    
    /**
     * Calculate the starting position of each raw argument in the input line
     */
    private static int[] calculateRawStartPositions(ArgumentQueue queue, String inputLine) {
        int[] positions = new int[queue.size()];
        int currentPos = 0;
        
        for (int i = 0; i < queue.size(); i++) {
            String rawArg = queue.get(i);
            
            // Find the next occurrence of this raw argument in the input line
            // Skip whitespace first
            while (currentPos < inputLine.length() && Character.isWhitespace(inputLine.charAt(currentPos))) {
                currentPos++;
            }
            
            // Handle quoted strings
            if (currentPos < inputLine.length() && isQuoteChar(inputLine.charAt(currentPos))) {
                // For quoted strings, the raw argument doesn't include quotes
                positions[i] = currentPos + 1; // Position after opening quote
                // Skip to after the closing quote
                currentPos = findClosingQuote(currentPos, inputLine) + 1;
            } else {
                // For unquoted arguments
                positions[i] = currentPos;
                currentPos += rawArg.length();
            }
        }
        
        return positions;
    }
    
    private static boolean isQuoteChar(char c) {
        return c == '"' || c == '\'';
    }
    
    private static int findClosingQuote(int openQuotePos, String inputLine) {
        char quoteChar = inputLine.charAt(openQuotePos);
        for (int i = openQuotePos + 1; i < inputLine.length(); i++) {
            if (inputLine.charAt(i) == quoteChar) {
                return i;
            }
        }
        return inputLine.length() - 1; // If no closing quote found, go to end
    }
    
    /**
     * Update cache when position changes - critical for performance
     */
    private void updateCache() {
        if (cacheValid &&
                lastParameterPosition == streamPosition.parameter &&
                lastRawPosition == streamPosition.raw) {
            return; // Cache is still valid
        }
        
        // Update parameter cache
        if (streamPosition.parameter >= parametersList.size()) {
            cachedCurrentParameter = null;
        } else {
            cachedCurrentParameter = parametersList.get(streamPosition.parameter);
        }
        
        // Update raw cache
        if (streamPosition.raw >= queue.size()) {
            cachedCurrentRaw = null;
        } else {
            cachedCurrentRaw = queue.get(streamPosition.raw);
        }
        
        lastParameterPosition = streamPosition.parameter;
        lastRawPosition = streamPosition.raw;
        cacheValid = true;
    }
    
    /**
     * Invalidate cache when position changes
     */
    private void invalidateCache() {
        cacheValid = false;
    }
    
    /**
     * Get the current letter position based on the current raw streamPosition position
     */
    private int getCurrentLetterPos() {
        if (streamPosition.raw >= rawStartPositions.length) {
            return inputLine.length();
        }
        return rawStartPositions[streamPosition.raw];
    }
    
    @Override
    public @NotNull StreamPosition<S> position() {
        return streamPosition;
    }
    
    // ============================================================================
    // FAST ACCESSOR METHODS (NO OPTIONAL OVERHEAD)
    // ============================================================================
    
    @Override
    public CommandParameter<S> currentParameterFast() {
        updateCache();
        return cachedCurrentParameter;
    }
    
    @Override
    public String currentRawFast() {
        updateCache();
        return cachedCurrentRaw;
    }
    
    @Override
    public CommandParameter<S> peekParameterFast() {
        int nextIndex = streamPosition.parameter + 1;
        if (nextIndex >= parametersList.size()) {
            return null;
        }
        return parametersList.get(nextIndex);
    }
    
    @Override
    public String peekRawFast() {
        int nextIndex = streamPosition.raw + 1;
        return queue.getOr(nextIndex, null);
    }
    
    @Override
    public Character currentLetterFast() {
        int letterPos = getCurrentLetterPos();
        if (letterPos >= inputLine.length()) {
            return null;
        }
        return inputLine.charAt(letterPos);
    }
    
    // ============================================================================
    // ORIGINAL OPTIONAL-BASED METHODS (FOR BACKWARD COMPATIBILITY)
    // ============================================================================
    
    @Override
    public Optional<CommandParameter<S>> popParameter() {
        streamPosition.shift(ShiftTarget.PARAMETER_ONLY, ShiftOperation.RIGHT);
        invalidateCache();
        CommandParameter<S> current = currentParameterFast();
        return Optional.ofNullable(current);
    }
    
    @Override
    public Optional<CommandParameter<S>> prevParameter() {
        if (streamPosition.parameter <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(parametersList.get(streamPosition.parameter - 1));
    }
    
    @Override
    public Optional<Character> peekLetter() {
        int letterPos = getCurrentLetterPos();
        int nextLetterPos = letterPos + 1;
        if (nextLetterPos >= inputLine.length()) {
            return Optional.empty();
        }
        return Optional.of(inputLine.charAt(nextLetterPos));
    }
    
    @Override
    public Optional<Character> popLetter() {
        int letterPos = getCurrentLetterPos();
        if (letterPos >= inputLine.length()) {
            return Optional.empty();
        }
        
        // Advance the position for the current raw argument
        if (streamPosition.raw < rawStartPositions.length) {
            rawStartPositions[streamPosition.raw]++;
        }
        
        return Optional.of(inputLine.charAt(letterPos));
    }
    
    @Override
    public Optional<String> popRaw() {
        streamPosition.shift(ShiftTarget.RAW_ONLY, ShiftOperation.RIGHT);
        invalidateCache();
        String current = currentRawFast();
        return Optional.ofNullable(current);
    }
    
    @Override
    public Optional<String> prevRaw() {
        int prev = streamPosition.raw - 1;
        return Optional.ofNullable(queue.getOr(prev, null));
    }
    
    @Override
    public boolean hasNextLetter() {
        return getCurrentLetterPos() < inputLine.length();
    }
    
    @Override
    public boolean hasNextRaw() {
        return streamPosition.canContinue(ShiftTarget.RAW_ONLY);
    }
    
    @Override
    public boolean hasPreviousRaw() {
        return streamPosition.raw > 0;
    }
    
    @Override
    public boolean hasNextParameter() {
        return streamPosition.canContinue(ShiftTarget.PARAMETER_ONLY);
    }
    
    @Override
    public boolean hasPreviousParameter() {
        return streamPosition.parameter > 0;
    }
    
    @Override
    public @NotNull ArgumentQueue getRawQueue() {
        return queue;
    }
    
    @Override
    public @NotNull List<CommandParameter<S>> getParametersList() {
        return parametersList;
    }
    
    @Override
    public boolean skip() {
        final StreamPosition<S> streamPosition = position();
        int prevRaw = streamPosition.raw;
        streamPosition.shift(ShiftTarget.ALL, ShiftOperation.RIGHT);
        invalidateCache();
        
        // The letter position is now automatically synchronized with the raw streamPosition
        // through getCurrentLetterPos()
        
        return streamPosition.raw > prevRaw;
    }
    
    @Override
    public boolean skipLetter() {
        return popLetter().isPresent();
    }
    
    /**
     * @return A copy of {@link CommandInputStream}
     */
    @Override
    public CommandInputStream<S> copy() {
        // Clone the array to avoid shared state
        return new CommandInputStreamImpl<>(
                this.queue.copy(),
                List.copyOf(parametersList),
                streamPosition.copy(),
                rawStartPositions.clone() // Clone the array to avoid shared state
        );
    }
}