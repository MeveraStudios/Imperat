package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.flow.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSyntaxException;

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

            //required
            return HandleResult.failure(new InvalidSyntaxException(CommandPathSearch.freshlyNew(context.getLastUsedCommand())));

        } catch (CommandException e) {
            return HandleResult.failure(e);
        }
    }

    private void handleEmptyOptional(Argument<S> optionalEmptyParameter, Cursor<S> stream,
            ExecutionContext<S> context) throws CommandException {
        if (optionalEmptyParameter.isFlag()) {
            FlagArgument<S> FlagArgument = optionalEmptyParameter.asFlagParameter();
            FlagData<S> flag = FlagArgument.flagData();
            Object value = null;

            if (flag.isSwitch()) {
                value = false;
            } else {
                var flagInputType = flag.inputType();
                assert flagInputType != null;

                String defaultStrValue = FlagArgument.getDefaultValueSupplier()
                                                 .supply(context, FlagArgument);
                if (defaultStrValue != null) {
                    value = flagInputType.parse(context, Cursor.subStream(stream, defaultStrValue), defaultStrValue);
                }
            }

            context.resolveFlag(flag, null, null, value);
        } else {
            context.resolveArgument(context.getLastUsedCommand(), null, stream.position().getParameter(),
                    optionalEmptyParameter, getDefaultValue(context, stream, optionalEmptyParameter));
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
