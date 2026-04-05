package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;

import java.util.List;
import java.util.Optional;

final class CursorImpl<S extends CommandSource> implements Cursor<S> {

    private final String inputLine;
    private final CursorPosition<S> cursorPosition;
    private final ArgumentInput queue;
    private final List<Argument<S>> parametersList;

    // Cache to store the starting position of each raw argument in the input line
    private final int[] rawStartPositions;

    Argument<S> cachedCurrentParameter = null;
    String cachedCurrentRaw = null;
    int lastParameterPosition = -1;
    int lastRawPosition = -1;
    boolean cacheValid = false;

    CursorImpl(ArgumentInput queue, CommandPathway<S> usage) {
        this(queue, usage.getArguments());
    }


    CursorImpl(ArgumentInput queue, List<Argument<S>> parameters) {
        this(queue, parameters, new CursorPosition<>(parameters.size(), queue.size()), calculateRawStartPositions(queue, queue.getOriginalRaw()));
    }

    CursorImpl(ArgumentInput queue, List<Argument<S>> parameters, CursorPosition<S> cursorPosition, int[] rawStartPositions) {
        this.queue = queue;
        this.inputLine = queue.getOriginalRaw();
        this.parametersList = parameters;
        this.cursorPosition = cursorPosition;
        this.rawStartPositions = rawStartPositions;
        updateCache(); // Initialize cache
    }

    /**
     * Calculate the starting position of each raw argument in the input line
     */
    private static int[] calculateRawStartPositions(ArgumentInput queue, String inputLine) {
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
                    lastParameterPosition == cursorPosition.parameter &&
                    lastRawPosition == cursorPosition.raw) {
            return; // Cache is still valid
        }

        // Update parameter cache
        if (cursorPosition.parameter >= parametersList.size()) {
            cachedCurrentParameter = null;
        } else {
            cachedCurrentParameter = parametersList.get(cursorPosition.parameter);
        }

        // Update raw cache
        if (cursorPosition.raw >= queue.size()) {
            cachedCurrentRaw = null;
        } else {
            cachedCurrentRaw = queue.get(cursorPosition.raw);
        }

        lastParameterPosition = cursorPosition.parameter;
        lastRawPosition = cursorPosition.raw;
        cacheValid = true;
    }

    /**
     * Invalidate cache when position changes
     */
    private void invalidateCache() {
        cacheValid = false;
    }

    /**
     * Get the current letter position based on the current raw cursorPosition position
     */
    private int getCurrentLetterPos() {
        if (cursorPosition.raw >= rawStartPositions.length) {
            return inputLine.length();
        }
        return rawStartPositions[cursorPosition.raw];
    }

    @Override
    public @NotNull CursorPosition<S> position() {
        return cursorPosition;
    }

    @Override
    public Argument<S> currentParameterIfPresent() {
        updateCache();
        return cachedCurrentParameter;
    }

    @Override
    public String currentRawIfPresent() {
        updateCache();
        return cachedCurrentRaw;
    }

    @Override
    public Argument<S> peekParameterIfPresent() {
        int nextIndex = cursorPosition.parameter + 1;
        if (nextIndex >= parametersList.size()) {
            return null;
        }
        return parametersList.get(nextIndex);
    }

    @Override
    public String peekRawIfPresent() {
        int nextIndex = cursorPosition.raw + 1;
        return queue.getOr(nextIndex, null);
    }

    @Override
    public Character currentLetterIfPresent() {
        int letterPos = getCurrentLetterPos();
        if (letterPos >= inputLine.length()) {
            return null;
        }
        return inputLine.charAt(letterPos);
    }

    @Override
    public @Nullable String nextInput() {
        return popRaw().orElse(null);
    }

    @Override
    public @Nullable Argument<S> nextParameter() {
        return popParameter().orElse(null);
    }

    @Override
    public Optional<Argument<S>> popParameter() {
        cursorPosition.shift(ShiftTarget.PARAMETER_ONLY, ShiftOperation.RIGHT);
        invalidateCache();
        Argument<S> current = currentParameterIfPresent();
        return Optional.ofNullable(current);
    }

    @Override
    public Optional<Argument<S>> prevParameter() {
        if (cursorPosition.parameter <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(parametersList.get(cursorPosition.parameter - 1));
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
        if (cursorPosition.raw < rawStartPositions.length) {
            rawStartPositions[cursorPosition.raw]++;
        }

        return Optional.of(inputLine.charAt(letterPos));
    }

    @Override
    public Optional<String> popRaw() {
        cursorPosition.shift(ShiftTarget.RAW_ONLY, ShiftOperation.RIGHT);
        invalidateCache();
        String current = currentRawIfPresent();
        return Optional.ofNullable(current);
    }

    @Override
    public Optional<String> prevRaw() {
        int prev = cursorPosition.raw - 1;
        return Optional.ofNullable(queue.getOr(prev, null));
    }

    @Override
    public boolean isCurrentLetterAvailable() {
        return getCurrentLetterPos() < inputLine.length();
    }

    @Override
    public boolean hasNextLetter() {
        return (getCurrentLetterPos() + 1) < inputLine.length();
    }

    @Override
    public boolean isCurrentRawInputAvailable() {
        return (cursorPosition.raw) < rawsLength();
    }

    @Override
    public boolean hasPreviousRaw() {
        return cursorPosition.raw > 0;
    }

    @Override
    public boolean hasNextRaw() {
        return (cursorPosition.raw + 1) < rawsLength();
    }

    @Override
    public boolean isCurrentParameterAvailable() {
        return (cursorPosition.parameter) < parametersLength();
    }

    @Override
    public boolean hasPreviousParameter() {
        return cursorPosition.parameter > 0;
    }

    @Override
    public boolean hasNextParameter() {
        return (cursorPosition.parameter + 1) < parametersLength();
    }

    @Override
    public @NotNull ArgumentInput getRawQueue() {
        return queue;
    }

    @Override
    public String collectRemainingRaw() {
        if (cursorPosition.raw >= queue.size()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(this.currentRaw().orElseThrow());
        while (this.hasNextRaw()) {
            this.popRaw()
                    .ifPresent(next -> sb.append(" ").append(next));
        }
        return sb.toString();
    }

    @Override
    public String collectRawArguments(int count) {
        if (count < 0) {
            return collectRemainingRaw();
        } else if (count == 0) {
            throw new IllegalArgumentException("Collecting no args ?!");
        }

        int consumed = 0;
        StringBuilder sb = new StringBuilder(this.currentRaw().orElseThrow());
        consumed++;

        while (this.hasNextRaw() && consumed < count) {
            var opt = popRaw();
            if (opt.isPresent()) {
                var nextRaw = opt.get();
                sb.append(" ").append(nextRaw);
                consumed++;
            }
        }
        return sb.toString();
    }

    @Override
    public @NotNull List<Argument<S>> getParametersList() {
        return parametersList;
    }

    @Override
    public boolean skip() {
        final CursorPosition<S> cursorPosition = position();
        int prevRaw = cursorPosition.raw;
        cursorPosition.shift(ShiftTarget.ALL, ShiftOperation.RIGHT);
        invalidateCache();

        // The letter position is now automatically synchronized with the raw cursorPosition
        // through getCurrentLetterPos()

        return cursorPosition.raw > prevRaw;
    }

    @Override
    public boolean skipLetter() {
        return popLetter().isPresent();
    }

    /**
     * @return A copy of {@link Cursor}
     */
    @Override
    public Cursor<S> copy() {
        // Clone the array to avoid shared state
        return new CursorImpl<>(
                this.queue.copy(),
                List.copyOf(parametersList),
                cursorPosition.copy(),
                rawStartPositions.clone() // Clone the array to avoid shared state
        );
    }

    @Override
    public void exemptParameter(Argument<S> matchingFlagParameter) {
        parametersList.remove(matchingFlagParameter);
    }

    @Override
    public void setAt(Cursor<S> cursorCopy) {
        this.cursorPosition.raw = cursorCopy.position().getRaw();
        this.cursorPosition.parameter = cursorCopy.position().getParameter();
        updateCache();
    }
}