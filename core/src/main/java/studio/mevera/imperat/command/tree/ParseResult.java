package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ParsedArgument;

public final class ParseResult<S extends CommandSource> {

    private final Object value;
    private final Throwable error;
    private final @Nullable Argument<S> argument;
    private final @Nullable String rawInput;
    private final int inputPosition;
    private final int nextDepth;

    private ParseResult(
            Object value,
            @Nullable Argument<S> argument,
            @Nullable String rawInput,
            int inputPosition,
            int nextDepth
    ) {
        this.value = value;
        this.error = null;
        this.argument = argument;
        this.rawInput = rawInput;
        this.inputPosition = inputPosition;
        this.nextDepth = nextDepth;
    }

    private ParseResult(
            Throwable error,
            @Nullable Argument<S> argument,
            @Nullable String rawInput,
            int inputPosition,
            int nextDepth
    ) {
        this.error = error;
        this.value = null;
        this.argument = argument;
        this.rawInput = rawInput;
        this.inputPosition = inputPosition;
        this.nextDepth = nextDepth;
    }

    public static <S extends CommandSource> ParseResult<S> successful(
            Object value,
            @Nullable Argument<S> argument,
            @Nullable String rawInput,
            int inputPosition,
            int nextDepth
    ) {
        return new ParseResult<>(value, argument, rawInput, inputPosition, nextDepth);
    }

    public static <S extends CommandSource> ParseResult<S> failed(Throwable ex) {
        return new ParseResult<>(ex, null, null, -1, -1);
    }

    public static <S extends CommandSource> ParseResult<S> failed(
            Throwable ex,
            @Nullable Argument<S> argument,
            @Nullable String rawInput,
            int inputPosition,
            int nextDepth
    ) {
        return new ParseResult<>(ex, argument, rawInput, inputPosition, nextDepth);
    }

    public boolean isSuccessful() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    public @Nullable Object getParsedValue() {
        return value;
    }

    /**
     * The exception thrown by {@link studio.mevera.imperat.command.arguments.type.ArgumentType#parse}
     * (or a structural error raised during parse orchestration). {@code null} on success.
     */
    public @Nullable Throwable getError() {
        return error;
    }

    public @Nullable Argument<S> getArgument() {
        return argument;
    }

    public @Nullable String getRawInput() {
        return rawInput;
    }

    public int getInputPosition() {
        return inputPosition;
    }

    public int getNextDepth() {
        return nextDepth;
    }

    public boolean canReuseInExecution() {
        return isSuccessful() && argument != null;
    }

    public @NotNull ParsedArgument<S> toParsedArgument() {
        if (!isSuccessful() || argument == null) {
            throw new IllegalStateException("This parse result does not carry a reusable parsed argument");
        }
        return new ParsedArgument<>(rawInput, argument, inputPosition, value);
    }
}
