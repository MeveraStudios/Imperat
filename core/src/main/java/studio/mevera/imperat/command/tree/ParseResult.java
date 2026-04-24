package studio.mevera.imperat.command.tree;

import static studio.mevera.imperat.command.tree.SuperCommandTree.FAILED_PARSE_SCORE;
import static studio.mevera.imperat.command.tree.SuperCommandTree.SUCCESSFUL_PARSE_SCORE;
import static studio.mevera.imperat.command.tree.SuperCommandTree.UNACCEPTABLE_SCORE;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;

public final class ParseResult<S extends CommandSource> {

    final @NotNull Argument<S> argument;
    final String input;
    final @Nullable Object parsedValue;
    final @Nullable Throwable error;

    int parseScore = FAILED_PARSE_SCORE;

    private ParseResult(@NotNull Argument<S> argument, String input, @Nullable Object parsedValue, @Nullable Throwable error) {
        this.argument = argument;
        this.input = input;
        this.parsedValue = parsedValue;
        this.error = error;
        this.parseScore = calculateParseScore();
    }

    public static <S extends CommandSource> ParseResult<S> of(@NotNull Argument<S> node, String input, @Nullable Object parsedValue,
            @Nullable Throwable error) {
        return new ParseResult<>(node, input, parsedValue, error);
    }

    public static <S extends CommandSource> ParseResult<S> unacceptableParse(@NotNull Argument<S> argument, String input,
            @NotNull Throwable error) {
        ParseResult<S> result = new ParseResult<>(argument, input, null, error);
        result.parseScore = UNACCEPTABLE_SCORE;
        return result;
    }

    public static <S extends CommandSource> ParseResult<S> failedParse(
            @NotNull Argument<S> argument,
            String input,
            @Nullable Throwable error
    ) {
        ParseResult<S> result = new ParseResult<>(argument, input, null, error);
        result.parseScore = FAILED_PARSE_SCORE;
        return result;
    }

    public @NotNull Argument<S> getArgument() {
        return argument;
    }

    public @Nullable Object getParsedValue() {
        return parsedValue;
    }

    public String getInput() {
        return input;
    }

    public @Nullable Throwable getError() {
        return error;
    }

    public boolean isUnAcceptableScore() {
        return parseScore == UNACCEPTABLE_SCORE;
    }

    public int getParseScore() {
        return parseScore;
    }

    private int calculateParseScore() {
        if (error != null) {
            //no match, return failure
            return FAILED_PARSE_SCORE;
        }

        return SUCCESSFUL_PARSE_SCORE;
    }

    public boolean isFailureScore() {
        return parseScore == FAILED_PARSE_SCORE;
    }
}
