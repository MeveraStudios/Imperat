package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Represents a stream of command input that provides sequential access to command arguments,
 * parameters, and individual characters. This interface allows for sophisticated navigation
 * and manipulation of command input data with cursor-based positioning.
 *
 * <p>The {@code CommandInputStream} operates on three distinct levels:
 * <ul>
 *   <li><strong>Character level:</strong> Individual character access within the input string</li>
 *   <li><strong>Raw input level:</strong> Space-separated argument strings</li>
 *   <li><strong>Parameter level:</strong> Typed command parameters with associated metadata</li>
 * </ul>
 *
 * <p>The stream maintains internal cursors for each level, allowing independent navigation
 * through the input data. This design enables complex parsing scenarios such as:
 * <ul>
 *   <li>Lookahead parsing without consuming input</li>
 *   <li>Backtracking to previous positions</li>
 *   <li>Conditional input consumption based on validation</li>
 *   <li>Character-by-character parsing for complex argument formats</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * CommandInputStream<Source> stream = CommandInputStream.of(
 *     ArgumentInput.of("player", "give", "diamond", "64"),
 *     detectedUsage
 * );
 *
 * // Navigate through parameters
 * while (stream.isCurrentParameterAvailable()) {
 *     CommandParameter<Source> param = stream.currentParameterIfPresent();
 *     String rawValue = stream.currentRawIfPresent();
 *
 *     // Process parameter...
 *     stream.skipParameter();
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> Implementations of this interface are not guaranteed
 * to be thread-safe. External synchronization is required when accessing the same stream
 * instance from multiple threads concurrently.
 *
 * @param <S> the type of the command source (e.g., Player, Console, CommandBlock)
 *           that initiated the command execution
 *
 * @author Mqzen
 * @since 1.0.0
 * @see CommandParameter
 * @see ArgumentInput
 * @see StreamPosition
 */
public interface CommandInputStream<S extends Source> {
    
    // ========================================
    // CORE POSITION AND STATE METHODS
    // ========================================
    
    /**
     * Retrieves the current cursor position within the input stream.
     * The position object contains cursors for all navigation levels
     * (character, raw input, and parameter).
     *
     * @return the current stream position, never {@code null}
     */
    @NotNull StreamPosition<S> position();
    
    /**
     * Creates a deep copy of this command input stream.
     * The copy maintains independent cursor positions and can be navigated
     * separately from the original stream.
     *
     * <p>This method is useful for implementing backtracking parsers or
     * for creating savepoints during complex parsing operations.
     *
     * @return a new {@link CommandInputStream} instance that is an independent
     *         copy of this stream
     */
    CommandInputStream<S> copy();
    
    // ========================================
    // CURRENT ELEMENT ACCESS METHODS
    // ========================================
    
    /**
     * Retrieves the command parameter at the current cursor position
     * without advancing the cursor.
     *
     * @return the current command parameter, or {@code null} if the cursor
     *         is beyond the end of the parameter list or no parameter exists
     */
    @Nullable
    CommandParameter<S> currentParameterIfPresent();
    
    /**
     * Retrieves the command parameter at the current cursor position
     * wrapped in an Optional container.
     *
     * @return an {@link Optional} containing the current command parameter,
     *         or {@link Optional#empty()} if none exists
     */
    @NotNull
    default Optional<CommandParameter<S>> currentParameter() {
        return Optional.ofNullable(currentParameterIfPresent());
    }
    
    /**
     * Retrieves the raw input string at the current cursor position
     * without advancing the cursor.
     *
     * @return the current raw input string, or {@code null} if the cursor
     *         is beyond the end of the input queue or no input exists
     */
    @Nullable
    String currentRawIfPresent();
    
    /**
     * Retrieves the raw input string at the current cursor position
     * wrapped in an Optional container.
     *
     * @return an {@link Optional} containing the current raw input,
     *         or {@link Optional#empty()} if none exists
     */
    @NotNull
    default Optional<String> currentRaw() {
        return Optional.ofNullable(currentRawIfPresent());
    }
    
    /**
     * Retrieves the character at the current cursor position within
     * the current raw input string without advancing the cursor.
     *
     * @return the current character, or {@code null} if the cursor is beyond
     *         the end of the current input string or no character exists
     */
    @Nullable
    Character currentLetterIfPresent();
    
    /**
     * Retrieves the character at the current cursor position wrapped
     * in an Optional container.
     *
     * @return an {@link Optional} containing the current character,
     *         or {@link Optional#empty()} if none exists
     */
    @NotNull
    default Optional<Character> currentLetter() {
        return Optional.ofNullable(currentLetterIfPresent());
    }
    
    // ========================================
    // PEEK METHODS (LOOKAHEAD)
    // ========================================
    
    /**
     * Peeks at the next command parameter without advancing the cursor.
     * This method allows lookahead parsing to determine the next parameter
     * type without consuming it.
     *
     * @return the next command parameter, or {@code null} if no next parameter exists
     */
    @Nullable
    CommandParameter<S> peekParameterIfPresent();
    
    /**
     * Peeks at the next command parameter wrapped in an Optional container.
     *
     * @return an {@link Optional} containing the next command parameter,
     *         or {@link Optional#empty()} if none exists
     */
    default Optional<CommandParameter<S>> peekParameter() {
        return Optional.ofNullable(peekParameterIfPresent());
    }
    
    /**
     * Peeks at the next raw input string without advancing the cursor.
     * This method allows lookahead parsing to examine the next input
     * without consuming it.
     *
     * @return the next raw input string, or {@code null} if no next input exists
     */
    @Nullable
    String peekRawIfPresent();
    
    /**
     * Peeks at the next raw input string wrapped in an Optional container.
     *
     * @return an {@link Optional} containing the next raw input,
     *         or {@link Optional#empty()} if none exists
     */
    default Optional<String> peekRaw() {
        return Optional.ofNullable(peekRawIfPresent());
    }
    
    /**
     * Peeks at the next character without advancing the cursor.
     * This method allows character-level lookahead parsing.
     *
     * @return an {@link Optional} containing the next character,
     *         or {@link Optional#empty()} if none exists
     */
    Optional<Character> peekLetter();
    
    // ========================================
    // PREVIOUS ELEMENT ACCESS METHODS
    // ========================================
    
    /**
     * Retrieves the previous command parameter without moving the cursor.
     * This method allows backward navigation through the parameter list.
     *
     * @return an {@link Optional} containing the previous command parameter,
     *         or {@link Optional#empty()} if no previous parameter exists
     */
    Optional<CommandParameter<S>> prevParameter();
    
    /**
     * Retrieves the previous raw input string without moving the cursor.
     * This method allows backward navigation through the input queue.
     *
     * @return an {@link Optional} containing the previous raw input,
     *         or {@link Optional#empty()} if no previous input exists
     */
    Optional<String> prevRaw();
    
    // ========================================
    // CONSUMING/ADVANCING METHODS
    // ========================================
    
    /**
     * Advances the cursor to the next parameter and returns it.
     * This method consumes the parameter and moves the cursor forward.
     *
     * @return the next command parameter, or {@code null} if no more parameters exist
     */
    @Nullable CommandParameter<S> nextParameter();
    
    /**
     * Advances the cursor to the next raw input and returns it.
     * This method consumes the input and moves the cursor forward.
     *
     * @return the next raw input string, or {@code null} if no more input exists
     */
    @Nullable String nextInput();
    
    /**
     * Removes and returns the current command parameter while advancing the cursor.
     * This method is equivalent to calling {@link #currentParameter()} followed
     * by {@link #skipParameter()}.
     *
     * @return an {@link Optional} containing the popped command parameter,
     *         or {@link Optional#empty()} if none exists
     */
    Optional<CommandParameter<S>> popParameter();
    
    /**
     * Removes and returns the current raw input while advancing the cursor.
     * This method is equivalent to calling {@link #currentRaw()} followed
     * by {@link #skipRaw()}.
     *
     * @return an {@link Optional} containing the popped raw input,
     *         or {@link Optional#empty()} if none exists
     */
    Optional<String> popRaw();
    
    /**
     * Removes and returns the current character while advancing the cursor.
     * This method is useful for character-by-character parsing of complex
     * argument formats.
     *
     * @return an {@link Optional} containing the popped character,
     *         or {@link Optional#empty()} if none exists
     */
    Optional<Character> popLetter();
    
    // ========================================
    // AVAILABILITY CHECK METHODS
    // ========================================
    
    /**
     * Checks if there is a command parameter available at the current cursor position.
     *
     * @return {@code true} if current parameter is available, {@code false} otherwise
     */
    boolean isCurrentParameterAvailable();
    
    /**
     * Checks if there is a raw input string available at the current cursor position.
     *
     * @return {@code true} if current raw input is available, {@code false} otherwise
     */
    boolean isCurrentRawInputAvailable();
    
    /**
     * Checks if there is a character available at the current cursor position
     * within the current raw input string.
     *
     * @return {@code true} if current character is available, {@code false} otherwise
     */
    boolean isCurrentLetterAvailable();
    
    /**
     * Checks if there is a next command parameter available in the stream.
     * This method allows checking for the availability of the next parameter
     * without consuming or peeking at it.
     *
     * @return {@code true} if a next command parameter exists, {@code false} otherwise
     */
    boolean hasNextParameter();
    
    /**
     * Checks if there is a next raw input string available in the stream.
     * This method allows checking for the availability of the next input
     * without consuming or peeking at it.
     *
     * @return {@code true} if a next raw input exists, {@code false} otherwise
     */
    boolean hasNextRaw();
    
    /**
     * Checks if there is another character available for reading in the
     * current input string.
     *
     * @return {@code true} if a next character exists, {@code false} otherwise
     */
    boolean hasNextLetter();
    
    /**
     * Checks if there is a previous command parameter available in the stream.
     *
     * @return {@code true} if a previous command parameter exists, {@code false} otherwise
     */
    boolean hasPreviousParameter();
    
    /**
     * Checks if there is a previous raw input string available in the stream.
     *
     * @return {@code true} if a previous raw input exists, {@code false} otherwise
     */
    boolean hasPreviousRaw();
    
    // ========================================
    // SKIP METHODS
    // ========================================
    
    /**
     * Skips the current input element and advances the appropriate cursor.
     * The specific behavior depends on the current stream position and
     * available input types.
     *
     * @return {@code true} if the skip operation was successful,
     *         {@code false} if no input was available to skip
     */
    boolean skip();
    
    /**
     * Skips the current command parameter and advances the parameter cursor.
     * This method moves to the next typed parameter in the parameter list.
     *
     * @return {@code true} if the skip operation was successful,
     *         {@code false} if no parameter was available to skip
     */
    default boolean skipParameter() {
        final StreamPosition<S> streamPosition = position();
        int prevParam = streamPosition.parameter;
        streamPosition.shiftRight(ShiftTarget.PARAMETER_ONLY);
        return streamPosition.parameter > prevParam;
    }
    
    /**
     * Skips the current raw input and advances the raw input cursor.
     * This method moves to the next space-separated argument in the input queue.
     *
     * @return {@code true} if the skip operation was successful,
     *         {@code false} if no raw input was available to skip
     */
    default boolean skipRaw() {
        final StreamPosition<S> streamPosition = position();
        int prevRaw = streamPosition.raw;
        streamPosition.shiftRight(ShiftTarget.RAW_ONLY);
        return streamPosition.raw > prevRaw;
    }
    
    /**
     * Skips the current character and advances the character cursor.
     * This method is used for character-level navigation within input strings.
     *
     * @return {@code true} if the skip operation was successful,
     *         {@code false} if no character was available to skip
     */
    boolean skipLetter();
    
    // ========================================
    // POSITION QUERY METHODS
    // ========================================
    
    /**
     * Retrieves the current position index of the parameter cursor.
     *
     * @return the zero-based index of the current parameter position
     */
    default int currentParameterPosition() {
        return position().parameter;
    }
    
    /**
     * Retrieves the current position index of the raw input cursor.
     *
     * @return the zero-based index of the current raw input position
     */
    default int currentRawPosition() {
        return position().raw;
    }
    
    /**
     * Retrieves the total number of command parameters in the stream.
     *
     * @return the total count of command parameters, always non-negative
     */
    default int parametersLength() {
        return getParametersList().size();
    }
    
    /**
     * Retrieves the total number of raw input strings in the stream.
     *
     * @return the total count of raw inputs, always non-negative
     */
    default int rawsLength() {
        return getRawQueue().size();
    }
    
    // ========================================
    // COLLECTION ACCESS METHODS
    // ========================================
    
    /**
     * Retrieves the complete list of command parameters associated with this stream.
     * This provides access to all parameter metadata for validation or processing.
     *
     * @return an immutable {@link List} of command parameters, never {@code null}
     */
    @NotNull List<CommandParameter<S>> getParametersList();
    
    /**
     * Retrieves the underlying queue containing all raw input strings.
     * This provides access to the complete input data for batch operations
     * or stream analysis.
     *
     * @return the {@link ArgumentInput} containing all raw inputs, never {@code null}
     */
    @NotNull ArgumentInput getRawQueue();
    
    // ========================================
    // UTILITY/HELPER METHODS
    // ========================================
    
    /**
     * Advances the character cursor until the specified target character is encountered.
     * The target character is also consumed (skipped) by this operation.
     *
     * <p>This method is useful for parsing delimited strings or finding specific
     * separators within command arguments.
     *
     * @param target the character to search for and skip to
     * @return {@code true} if the target character was found and reached,
     *         {@code false} if the end of input was reached without finding the target
     */
    default boolean skipTill(char target) {
        boolean reached = false;
        
        while (hasNextLetter()) {
            Character current = currentLetterIfPresent();
            if (current != null && current == target) {
                reached = true;
                break;
            }
            popLetter();
        }
        // Skipping current letter (which equals the target)
        skipLetter();
        return reached;
    }
    
    /**
     * Collects and returns all characters before the first occurrence of the specified character.
     * The character cursor is advanced to the position just before the target character.
     * The target character itself is not consumed or included in the result.
     *
     * <p>This method is useful for extracting string tokens that are delimited by
     * specific characters.
     *
     * @param c the delimiter character to stop collection at
     * @return a string containing all characters collected before the delimiter,
     *         or an empty string if the delimiter is immediately encountered
     *         or no characters are available
     */
    default String collectBeforeFirst(char c) {
        StringBuilder builder = new StringBuilder();
        while (hasNextLetter()) {
            Character current = currentLetterIfPresent();
            if (current == null || current == c) {
                break;
            }
            builder.append(current);
            skipLetter();
        }
        return builder.toString();
    }
    
    /**
     * Reads and returns the current raw input without advancing the cursor.
     * This method provides a fail-fast alternative to {@link #currentRawIfPresent()}
     * when the presence of input is guaranteed.
     *
     * @return the current raw input string, never {@code null}
     * @throws NoSuchElementException if no raw input is available at the current position
     */
    default String readInput() {
        String current = currentRawIfPresent();
        if (current == null) {
            throw new NoSuchElementException("No raw input available");
        }
        return current;
    }
    
    default void endInput(){
        position().raw = position().maxRawLength;
    }
    
    /**
     * Marks the specified parameter as exempt from normal processing.
     * Exempt parameters are typically flag parameters that have been
     * handled separately from the main argument processing flow.
     *
     * <p>This method allows the stream to track which parameters have
     * been processed outside the normal sequential flow, enabling
     * proper validation and error reporting.
     *
     * @param matchingFlagParameter the parameter to mark as exempt
     * @throws IllegalArgumentException if matchingFlagParameter is {@code null}
     */
    void exemptParameter(CommandParameter<S> matchingFlagParameter);
    
    // ========================================
    // STATIC FACTORY METHODS
    // ========================================
    
    /**
     * Creates a new {@link CommandInputStream} instance with the specified raw arguments
     * and command usage information. The usage information determines how the raw
     * arguments are mapped to typed command parameters.
     *
     * <p>This factory method is the primary way to create command input streams
     * from parsed command input and detected command structure.
     *
     * @param <S> the type of command source
     * @param queue the queue containing all raw argument strings
     * @param usage the command usage definition that describes expected parameters
     * @return a new {@link CommandInputStream} instance initialized with the
     *         provided data
     * @throws IllegalArgumentException if queue or usage is {@code null}
     */
    static <S extends Source> CommandInputStream<S> of(ArgumentInput queue, CommandUsage<S> usage) {
        return new CommandInputStreamImpl<>(queue, usage);
    }
    
    /**
     * Creates a new {@link CommandInputStream} with a single string as input.
     * This factory method is useful for creating streams for single-argument
     * parsing or testing scenarios.
     *
     * @param <S> the type of command source
     * @param parameter the command parameter associated with the input string
     * @param str the raw input string to be processed
     * @return a new {@link CommandInputStream} instance containing the single input
     * @throws IllegalArgumentException if parameter or str is {@code null}
     */
    static <S extends Source> CommandInputStream<S> ofSingleString(@NotNull CommandParameter<S> parameter, @NotNull String str) {
        return new CommandInputStreamImpl<>(ArgumentInput.of(str), List.of(parameter));
    }
    
    /**
     * Creates a substream of the specified parent stream with new input content.
     * The substream inherits the current parameter context from the parent stream
     * but operates on the new input string.
     *
     * <p>This method is useful for recursive parsing scenarios where a parameter
     * value needs to be parsed as a nested command structure.
     *
     * @param <S> the type of command source
     * @param stream the parent command input stream to derive context from
     * @param input the raw input string for the new substream
     * @return a new {@link CommandInputStream} instance representing the substream
     * @throws NoSuchElementException if the parent stream has no current parameter available
     * @throws IllegalArgumentException if stream or input is {@code null}
     */
    static <S extends Source> CommandInputStream<S> subStream(@NotNull CommandInputStream<S> stream, @NotNull String input) {
        CommandParameter<S> param = stream.currentParameterIfPresent();
        if (param == null) {
            throw new NoSuchElementException("No current parameter available");
        }
        return ofSingleString(param, input);
    }
}