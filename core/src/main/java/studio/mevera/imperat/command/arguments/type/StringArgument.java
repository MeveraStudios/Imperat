package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.priority.Priority;

import java.util.Set;

public final class StringArgument<S extends CommandSource> extends ArgumentType<S, String> {
    StringArgument() {
        super();
    }

    @Override
    public String parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws CommandException {
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Input is empty");
        }
        return input;
    }

    @Override
    public String parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws CommandException {
        String input = cursor.currentRawIfPresent();
        if (input == null) {
            throw new IllegalArgumentException("No input available at cursor position");
        }
        final Argument<S> parameter = cursor.currentParameter().orElse(null);
        if (canUseFastPath(parameter, input)) {
            return input;
        }
        return resolveWithPrecision(context, cursor, input, parameter);
    }

    private boolean canUseFastPath(Argument<S> parameter, String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        if (parameter != null && parameter.isGreedyString()) {
            return false;
        }
        return !studio.mevera.imperat.util.StringUtils.isQuoteChar(input.charAt(0));
    }

    private String resolveWithPrecision(ExecutionContext<S> context, Cursor<S> cursor, String input, Argument<S> argument)
            throws CommandException {
        StringBuilder builder = new StringBuilder();
        final Character current = cursor.currentLetter().orElse(null);
        if (current == null) {
            return input;
        }
        if (argument != null && argument.isGreedyString()) {
            int limit = -1;
            if (argument.isAnnotated()) {
                Greedy greedyAnn = argument.asAnnotatedArgument().getAnnotation(Greedy.class);
                if (greedyAnn == null) {
                    throw new CommandException(
                            "Greedy annotation missing on an annotated argument marked as greedy string, there's something wrong!");
                }
                limit = greedyAnn.limit();
            }
            handleGreedyOptimized(builder, limit, cursor, context);
            return builder.toString();
        }
        Character next;
        do {
            next = cursor.popLetter().orElse(null);
            if (next == null) {
                break;
            }
            builder.append(next);
        } while (cursor.isCurrentRawInputAvailable()
                         && cursor.peekLetter().map((ch) -> !studio.mevera.imperat.util.StringUtils.isQuoteChar(ch))
                                    .orElse(false));
        return builder.toString();
    }
    private void handleGreedyOptimized(StringBuilder builder, int limit, Cursor<S> inputStream, ExecutionContext<S> context) throws CommandException {
        Argument<S> nextParam = GreedyLimitHelper.findNextNonFlagParam(inputStream);
        boolean nextParamCanDiscriminate = nextParam != null && !nextParam.isGreedyString()
                                                   && !(nextParam.type() instanceof StringArgument<?>);
        int effectiveLimit = GreedyLimitHelper.computeEffectiveLimit(
                limit, nextParam, nextParamCanDiscriminate, inputStream
        );
        int consumed = 0;
        while (inputStream.isCurrentRawInputAvailable()) {
            String candidate = inputStream.currentRaw().orElse(null);
            if (candidate == null) {
                return;
            }
            if (Patterns.isInputFlag(candidate)) {
                Set<FlagArgument<S>> extracted = context.getDetectedPathway().getFlagExtractor().extract(candidate);
                if (!extracted.isEmpty()) {
                    inputStream.skipRaw();
                    if (extracted.stream().noneMatch(FlagArgument::isSwitch)) {
                        inputStream.skipRaw();
                    }
                    continue;
                }
            }
            break;
        }
        String firstRaw = inputStream.currentRaw().orElse(null);
        if (firstRaw == null) {
            return;
        }
        builder.append(firstRaw);
        consumed++;
        while (inputStream.hasNextRaw()) {
            if (effectiveLimit > 0 && consumed >= effectiveLimit) {
                break;
            }
            String peeked = inputStream.peekRawIfPresent();
            if (peeked == null) {
                break;
            }
            if (Patterns.isInputFlag(peeked)) {
                Set<FlagArgument<S>> extracted = context.getDetectedPathway().getFlagExtractor().extract(peeked);
                if (!extracted.isEmpty()) {
                    inputStream.skipRaw();
                    inputStream.skipRaw();
                    if (extracted.stream().noneMatch(FlagArgument::isSwitch)) {
                        inputStream.skipRaw();
                    }
                    continue;
                }
            }
            if (nextParamCanDiscriminate) {
                int peekRawPos = inputStream.currentRawPosition() + 1;
                String peekedInput = context.arguments().getOr(peekRawPos, null);
                if (peekedInput != null) {
                    try {
                        nextParam.type().parse(context, nextParam, peekedInput);
                        break;
                    } catch (Exception ignored) {
                        // Not a match, continue
                    }
                }
            }
            inputStream.skipRaw();
            builder.append(" ").append(peeked);
            consumed++;
        }
    }
    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return parameter.isGreedyString();
    }
    @Override
    public int getNumberOfParametersToConsume(Argument<S> argument) {
        return 1;
    }
    @Override
    public @NotNull Priority getPriority() {
        return Priority.LOW;
    }
}
