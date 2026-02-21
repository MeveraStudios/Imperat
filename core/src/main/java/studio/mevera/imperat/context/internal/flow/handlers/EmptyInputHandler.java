package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.ParsedFlagArgument;
import studio.mevera.imperat.context.internal.flow.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;

public final class EmptyInputHandler<S extends Source> implements ParameterHandler<S> {

    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, Cursor<S> stream) throws CommandException {
        Argument<S> currentParameter = stream.currentParameterIfPresent();
        if (currentParameter == null) {
            return HandleResult.TERMINATE;
        }

        String currentRaw = stream.currentRawIfPresent();
        if (currentRaw != null) {
            return HandleResult.NEXT_HANDLER; // Not empty input, let other handlers process
        }

        try {

            if (currentParameter.isOptional()) {
                handleEmptyOptional(currentParameter, stream, context);
                stream.skipParameter();
                return HandleResult.NEXT_ITERATION;
            }

            //required parameter with no input - invalid syntax
            var command = context.getLastUsedCommand();
            var usage = command != null ? command.getDefaultPathway() : null;
            var exception = new CommandException(ResponseKey.INVALID_SYNTAX);
            if (usage != null) {
                exception.withPlaceholder("closest_usage", context.imperatConfig().commandPrefix() + CommandPathway.format(command, usage));
            } else {
                exception.withPlaceholder("closest_usage", "");
            }
            return HandleResult.failure(exception);

        } catch (CommandException e) {
            return HandleResult.failure(e);
        }
    }

    private void handleEmptyOptional(Argument<S> optionalEmptyParameter, Cursor<S> stream,
            ExecutionContext<S> context) throws CommandException {
        if (optionalEmptyParameter.isFlag()) {
            FlagArgument<S> flagArgument = optionalEmptyParameter.asFlagParameter();
            FlagData<S> flag = flagArgument.flagData();

            ParsedFlagArgument<S> parsedFlagArgument;
            if (flag.isSwitch()) {
                parsedFlagArgument = ParsedFlagArgument.forDefaultSwitch(flagArgument);
            } else {
                var flagInputType = flag.inputType();
                assert flagInputType != null;

                String defaultStrValue = flagArgument.getDefaultValueSupplier()
                                                 .supply(context, flagArgument);
                if (defaultStrValue != null) {
                    java.lang.Object flagInputValue = flagInputType.parse(context, Cursor.subStream(stream, defaultStrValue), defaultStrValue);
                    parsedFlagArgument = ParsedFlagArgument.forDefaultFlag(flagArgument, defaultStrValue, flagInputValue);
                }else {
                    parsedFlagArgument = ParsedFlagArgument.forDefaultFlag(flagArgument, "null", null);
                }
            }
            context.resolveFlag(parsedFlagArgument);

        } else {
            context.resolveArgument(
                    new ParsedArgument<>(
                            null,
                            optionalEmptyParameter,
                            stream.position().getParameter(),
                            getDefaultValue(context, stream, optionalEmptyParameter)
                    )
            );
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getDefaultValue(ExecutionContext<S> context, Cursor<S> stream, Argument<S> parameter) throws CommandException {
        OptionalValueSupplier optionalSupplier = parameter.getDefaultValueSupplier();
        if (optionalSupplier.isEmpty()) {
            return null;
        }
        String value = optionalSupplier.supply(context, parameter);

        if (value != null) {
            return (T) parameter.type().parse(context, stream, value);
        }

        return null;
    }
}
