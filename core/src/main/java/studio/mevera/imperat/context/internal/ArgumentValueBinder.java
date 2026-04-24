package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeWrap;

final class ArgumentValueBinder {

    private static final TypeWrap<String> STRING_TYPE = TypeWrap.of(String.class);

    private ArgumentValueBinder() {
    }

    static <S extends CommandSource> void bindCurrentParameter(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor
    ) throws CommandException {
        Argument<S> argument = requireCurrentParameter(cursor);
        Cursor<S> working = cursor.copy();
        String input = collectInput(context, working, argument);
        Object value = argument.type().parse(context, argument, input);

        context.parseArgument(new ParsedArgument<>(
                input,
                argument,
                cursor.currentParameterPosition(),
                value
        ));

        cursor.setAt(working);
        cursor.skipParameter();
    }

    static <S extends CommandSource> void bindParsedParameter(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor,
            @NotNull ParseResult<S> parseResult
    ) throws CommandException {
        Argument<S> argument = requireCurrentParameter(cursor);
        if (parseResult.getArgument() != argument) {
            throw new IllegalStateException("Pre-parsed argument does not match the current cursor parameter");
        }

        context.parseArgument(parseResult.toParsedArgument());

        while (cursor.currentRawPosition() < parseResult.getNextDepth()) {
            cursor.skipRaw();
        }
        cursor.skipParameter();
    }

    static <S extends CommandSource> boolean skipCurrentFlag(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor
    ) {
        String currentRaw = cursor.currentRawIfPresent();
        if (currentRaw == null || !Patterns.isInputFlag(currentRaw)) {
            return false;
        }

        FlagData<S> flagData = context.getDetectedPathway().getFlagDataFromInput(currentRaw);
        if (flagData == null) {
            return false;
        }

        cursor.skipRaw();
        if (!flagData.isSwitch()) {
            cursor.skipRaw();
        }
        return true;
    }

    static <S extends CommandSource> void bindCurrentSubCommand(
            @NotNull Cursor<S> cursor
    ) throws CommandException {
        Argument<S> currentParameter = cursor.currentParameterIfPresent();
        String currentRaw = cursor.currentRawIfPresent();
        if (currentParameter == null || currentRaw == null || !currentParameter.isCommand()) {
            return;
        }
        if (!currentParameter.asCommand().hasName(currentRaw)) {
            throw new CommandException("Invalid sub-command: '" + currentRaw + "'");
        }

        cursor.skip();
    }

    private static <S extends CommandSource> @NotNull Argument<S> requireCurrentParameter(Cursor<S> cursor) {
        Argument<S> argument = cursor.currentParameterIfPresent();
        if (argument == null) {
            throw new IllegalStateException("No current parameter is available for binding");
        }
        return argument;
    }

    private static <S extends CommandSource> String collectInput(
            ExecutionContext<S> context,
            Cursor<S> working,
            Argument<S> argument
    ) throws CommandException {
        if (argument.isGreedy() || argument.type().isGreedy(argument)) {
            return collectGreedyInput(context, working, argument);
        }
        return collectFixedInput(context, working, argument);
    }

    private static <S extends CommandSource> String collectFixedInput(
            ExecutionContext<S> context,
            Cursor<S> working,
            Argument<S> argument
    ) throws CommandException {
        int target = Math.max(1, argument.type().getNumberOfParametersToConsume(argument));
        StringBuilder builder = new StringBuilder();
        int consumed = 0;

        while (working.isCurrentRawInputAvailable() && consumed < target) {
            if (skipCurrentFlag(context, working)) {
                continue;
            }

            String raw = working.currentRawIfPresent();
            if (raw == null) {
                break;
            }

            if (consumed > 0) {
                builder.append(' ');
            }
            builder.append(raw);
            consumed++;
            working.skipRaw();
        }

        if (consumed == 0) {
            throw new IllegalArgumentException("No input available at cursor position");
        }
        return builder.toString();
    }

    private static <S extends CommandSource> String collectGreedyInput(
            ExecutionContext<S> context,
            Cursor<S> working,
            Argument<S> argument
    ) throws CommandException {
        int limit = argument.greedyLimit();
        Argument<S> nextParam = findNextNonFlagParameter(working);
        boolean nextParamCanDiscriminate = canDiscriminate(nextParam);
        int effectiveLimit = computeEffectiveGreedyLimit(context, working, limit, nextParam, nextParamCanDiscriminate);

        StringBuilder builder = new StringBuilder();
        int consumed = 0;

        while (working.isCurrentRawInputAvailable()) {
            if (skipCurrentFlag(context, working)) {
                continue;
            }

            String raw = working.currentRawIfPresent();
            if (raw == null) {
                break;
            }

            if (consumed > 0) {
                if (effectiveLimit > 0 && consumed >= effectiveLimit) {
                    break;
                }

                if (nextParamCanDiscriminate && matchesParameterType(context, nextParam, raw)) {
                    break;
                }
                builder.append(' ');
            }

            builder.append(raw);
            consumed++;
            working.skipRaw();
        }

        if (consumed == 0) {
            throw new IllegalArgumentException("No input available at cursor position");
        }
        return builder.toString();
    }

    private static <S extends CommandSource> @Nullable Argument<S> findNextNonFlagParameter(Cursor<S> cursor) {
        int currentPos = cursor.currentParameterPosition();
        for (int i = currentPos + 1; i < cursor.parametersLength(); i++) {
            Argument<S> param = cursor.getParametersList().get(i);
            if (!param.isFlag()) {
                return param;
            }
        }
        return null;
    }

    private static <S extends CommandSource> boolean canDiscriminate(@Nullable Argument<S> argument) {
        if (argument == null || argument.isGreedyString()) {
            return false;
        }
        return !STRING_TYPE.isSupertypeOf(argument.valueType());
    }

    private static <S extends CommandSource> boolean matchesParameterType(
            ExecutionContext<S> context,
            @Nullable Argument<S> argument,
            String input
    ) {
        if (argument == null) {
            return false;
        }
        try {
            argument.type().parse(context, argument, input);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static <S extends CommandSource> int computeEffectiveGreedyLimit(
            ExecutionContext<S> context,
            Cursor<S> cursor,
            int greedyLimit,
            @Nullable Argument<S> nextParam,
            boolean nextParamCanDiscriminate
    ) throws CommandException {
        if (nextParam != null && !nextParamCanDiscriminate) {
            int reserve = countRemainingRequiredParams(cursor);
            int available = countRemainingBindableRaws(context, cursor.copy());
            int maxByReserve = available - reserve;
            int effectiveLimit = greedyLimit > 0 ? Math.min(greedyLimit, maxByReserve) : maxByReserve;
            return Math.max(effectiveLimit, 1);
        }
        return greedyLimit;
    }

    private static <S extends CommandSource> int countRemainingRequiredParams(Cursor<S> cursor) {
        int count = 0;
        int currentPos = cursor.currentParameterPosition();
        for (int i = currentPos + 1; i < cursor.parametersLength(); i++) {
            Argument<S> param = cursor.getParametersList().get(i);
            if (param.isRequired() && !param.isFlag()) {
                count++;
            }
        }
        return count;
    }

    private static <S extends CommandSource> int countRemainingBindableRaws(
            ExecutionContext<S> context,
            Cursor<S> working
    ) throws CommandException {
        int count = 0;
        while (working.isCurrentRawInputAvailable()) {
            if (skipCurrentFlag(context, working)) {
                continue;
            }
            count++;
            working.skipRaw();
        }
        return count;
    }
}
