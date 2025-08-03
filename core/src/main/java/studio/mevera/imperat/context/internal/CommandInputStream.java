package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.tree.CommandTree;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Represents a stream of command input, allowing navigation and manipulation
 * of raw input, parameters, and individual characters.
 *
 * PERFORMANCE NOTE: Use fast accessor methods (ending with Fast) for performance-critical code
 * to avoid Optional object allocation overhead.
 *
 * @param <S> The type of the source associated with the command input stream.
 */
public interface CommandInputStream<S extends Source> {
    
    /**
     * Retrieves the current cursor position in the stream.
     *
     * @return The current cursor.
     */
    @NotNull StreamPosition<S> position();
    
    // ============================================================================
    // FAST ACCESSOR METHODS (NO OPTIONAL OVERHEAD) - USE THESE FOR PERFORMANCE
    // ============================================================================
    
    /**
     * FAST: Retrieves the current command parameter at the cursor position.
     * Returns null if none exists. Use this for performance-critical code.
     *
     * @return The current command parameter, or null if none exists.
     */
    @Nullable
    CommandParameter<S> currentParameterFast();
    
    /**
     * FAST: Retrieves the current raw input at the cursor position.
     * Returns null if none exists. Use this for performance-critical code.
     *
     * @return The current raw input, or null if none exists.
     */
    @Nullable
    String currentRawFast();
    
    /**
     * FAST: Peeks at the next command parameter without advancing the cursor.
     * Returns null if none exists. Use this for performance-critical code.
     *
     * @return The next command parameter, or null if none exists.
     */
    @Nullable
    CommandParameter<S> peekParameterFast();
    
    /**
     * FAST: Peeks at the next raw input without advancing the cursor.
     * Returns null if none exists. Use this for performance-critical code.
     *
     * @return The next raw input, or null if none exists.
     */
    @Nullable
    String peekRawFast();
    
    /**
     * FAST: Retrieves the current character at the cursor position.
     * Returns null if none exists. Use this for performance-critical code.
     *
     * @return The current character, or null if none exists.
     */
    @Nullable
    Character currentLetterFast();
    
    // ============================================================================
    // ORIGINAL OPTIONAL-BASED METHODS (BACKWARD COMPATIBILITY)
    // ============================================================================
    
    /**
     * Retrieves the current command parameter at the cursor position.
     *
     * @return An {@link Optional} containing the current command parameter, or empty if none exists.
     */
    @NotNull
    default Optional<CommandParameter<S>> currentParameter() {
        return Optional.ofNullable(currentParameterFast());
    }
    
    /**
     * Peeks at the next command parameter without advancing the cursor.
     *
     * @return An {@link Optional} containing the next command parameter, or empty if none exists.
     */
    default Optional<CommandParameter<S>> peekParameter() {
        return Optional.ofNullable(peekParameterFast());
    }
    
    /**
     * Pops the current command parameter and advances the cursor.
     *
     * @return An {@link Optional} containing the popped command parameter, or empty if none exists.
     */
    Optional<CommandParameter<S>> popParameter();
    
    /**
     * Retrieves the previous command parameter without moving the cursor.
     *
     * @return An {@link Optional} containing the previous command parameter, or empty if none exists.
     */
    Optional<CommandParameter<S>> prevParameter();
    
    /**
     * Retrieves the current character at the cursor position.
     *
     * @return An {@link Optional} containing the current character, or empty if none exists.
     */
    @NotNull
    default Optional<Character> currentLetter() {
        return Optional.ofNullable(currentLetterFast());
    }
    
    /**
     * Peeks at the next character without advancing the cursor.
     *
     * @return An {@link Optional} containing the next character, or empty if none exists.
     */
    Optional<Character> peekLetter();
    
    /**
     * Pops the current character and advances the cursor.
     *
     * @return An {@link Optional} containing the popped character, or empty if none exists.
     */
    Optional<Character> popLetter();
    
    /**
     * Retrieves the current raw input at the cursor position.
     *
     * @return An {@link Optional} containing the current raw input, or empty if none exists.
     */
    @NotNull
    default Optional<String> currentRaw() {
        return Optional.ofNullable(currentRawFast());
    }
    
    /**
     * Peeks at the next raw input without advancing the cursor.
     *
     * @return An {@link Optional} containing the next raw input, or empty if none exists.
     */
    default Optional<String> peekRaw() {
        return Optional.ofNullable(peekRawFast());
    }
    
    /**
     * Pops the current raw input and advances the cursor.
     *
     * @return An {@link Optional} containing the popped raw input, or empty if none exists.
     */
    Optional<String> popRaw();
    
    /**
     * Retrieves the previous raw input without moving the cursor.
     *
     * @return An {@link Optional} containing the previous raw input, or empty if none exists.
     */
    Optional<String> prevRaw();
    
    /**
     * Checks if there is a next character available in the stream.
     *
     * @return True if a next character exists, false otherwise.
     */
    boolean hasNextLetter();
    
    /**
     * Checks if there is a next raw input available in the stream.
     *
     * @return True if a next raw input exists, false otherwise.
     */
    boolean hasNextRaw();
    
    /**
     * Checks if there is a previous raw input available in the stream.
     *
     * @return True if a previous raw input exists, false otherwise.
     */
    boolean hasPreviousRaw();
    
    /**
     * Checks if there is a next command parameter available in the stream.
     *
     * @return True if a next command parameter exists, false otherwise.
     */
    boolean hasNextParameter();
    
    /**
     * Checks if there is a previous command parameter available in the stream.
     *
     * @return True if a previous command parameter exists, false otherwise.
     */
    boolean hasPreviousParameter();
    
    /**
     * Retrieves the queue of raw inputs.
     *
     * @return The {@link ArgumentInput} containing raw inputs.
     */
    @NotNull ArgumentInput getRawQueue();
    
    /**
     * Retrieves the list of command parameters.
     *
     * @return A {@link List} of command parameters.
     */
    @NotNull List<CommandParameter<S>> getParametersList();
    
    /**
     * Skips the current input (raw or parameter) and advances the cursor.
     *
     * @return True if the skip was successful, false otherwise.
     */
    boolean skip();
    
    /**
     * Skips the current character and advances the cursor.
     *
     * @return True if the skip was successful, false otherwise.
     */
    boolean skipLetter();
    
    /**
     * Skips the current raw input and advances the cursor.
     *
     * @return True if the skip was successful, false otherwise.
     */
    default boolean skipRaw() {
        final StreamPosition<S> streamPosition = position();
        int prevRaw = streamPosition.raw;
        streamPosition.shiftRight(ShiftTarget.RAW_ONLY);
        return streamPosition.raw > prevRaw;
    }
    
    /**
     * Skips the current command parameter and advances the cursor.
     *
     * @return True if the skip was successful, false otherwise.
     */
    default boolean skipParameter() {
        final StreamPosition<S> streamPosition = position();
        int prevParam = streamPosition.parameter;
        streamPosition.shiftRight(ShiftTarget.PARAMETER_ONLY);
        return streamPosition.parameter > prevParam;
    }
    
    /**
     * Retrieves the current position of the raw input cursor.
     *
     * @return The current raw input position.
     */
    default int currentRawPosition() {
        return position().raw;
    }
    
    /**
     * Retrieves the current position of the parameter cursor.
     *
     * @return The current parameter position.
     */
    default int currentParameterPosition() {
        return position().parameter;
    }
    
    /**
     * Retrieves the total number of raw inputs in the stream.
     *
     * @return The number of raw inputs.
     */
    default int rawsLength() {
        return getRawQueue().size();
    }
    
    /**
     * Retrieves the total number of command parameters in the stream.
     *
     * @return The number of command parameters.
     */
    default int parametersLength() {
        return getParametersList().size();
    }
    
    /**
     * Skips characters in the stream until the specified target character is reached.
     *
     * @param target The target character to skip to.
     * @return True if the target character was reached, false otherwise.
     */
    default boolean skipTill(char target) {
        boolean reached = false;
        
        while (hasNextLetter()) {
            Character current = currentLetterFast();
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
     * Collects all characters before the first occurrence of the specified character.
     *
     * @param c The character to stop collecting at.
     * @return A string containing the collected characters.
     */
    default String collectBeforeFirst(char c) {
        StringBuilder builder = new StringBuilder();
        while (hasNextLetter()) {
            Character current = currentLetterFast();
            if (current == null || current == c) {
                break;
            }
            builder.append(current);
            skipLetter();
        }
        return builder.toString();
    }
    
    /**
     * @return A copy of {@link CommandInputStream}
     */
    CommandInputStream<S> copy();
    
    /**
     * Reads the current raw input.
     *
     * @return The current raw input.
     * @throws NoSuchElementException if no raw input is available.
     */
    default String readInput() {
        String current = currentRawFast();
        if (current == null) {
            throw new NoSuchElementException("No raw input available");
        }
        return current;
    }
    
    /**
     * Creates a new {@link CommandInputStream} with queue of all raw arguments
     * and from the usage detected from the raw input by {@link CommandTree}.
     * @param queue the queue of all raw arguments.
     * @param usage the usage detected.
     * @return the stream containing pointers for raw input and parameter input.
     * @param <S> the type of source/command-sender.
     */
    static <S extends Source> CommandInputStream<S> of(ArgumentInput queue, CommandUsage<S> usage) {
        return new CommandInputStreamImpl<>(queue, usage);
    }
    
    /**
     * Creates a new {@link CommandInputStream} with a single string as input.
     *
     * @param parameter The command parameter associated with the input.
     * @param str       The raw input string.
     * @param <S>       The type of the source.
     * @return A new {@link CommandInputStream} instance.
     */
    static <S extends Source> CommandInputStream<S> ofSingleString(@NotNull CommandParameter<S> parameter, @NotNull String str) {
        return new CommandInputStreamImpl<>(ArgumentInput.of(str), List.of(parameter));
    }
    
    /**
     * Creates a substream of the current {@link CommandInputStream} with the specified input.
     *
     * @param stream The parent command input stream.
     * @param input  The raw input for the substream.
     * @param <S>    The type of the source.
     * @return A new {@link CommandInputStream} instance representing the substream.
     * @throws NoSuchElementException if the current parameter is not available.
     */
    static <S extends Source> CommandInputStream<S> subStream(@NotNull CommandInputStream<S> stream, @NotNull String input) {
        CommandParameter<S> param = stream.currentParameterFast();
        if (param == null) {
            throw new NoSuchElementException("No current parameter available");
        }
        return ofSingleString(param, input);
    }
    
    void exemptParameter(CommandParameter<S> matchingFlagParameter);
}