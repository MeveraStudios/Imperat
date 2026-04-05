package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.DefaultValueProvider;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.command.tree.TreeExecutionResult;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.ParsedFlagArgument;
import studio.mevera.imperat.context.internal.flow.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSyntaxException;

public final class EmptyInputHandler<S extends CommandSource> implements ParameterHandler<S> {

    @Override
    public @NotNull HandleResult handle(TreeExecutionResult<S> result, ExecutionContext<S> context, Cursor<S> cursor) throws CommandException {
        Argument<S> currentParameter = cursor.currentParameterIfPresent();
        if (currentParameter == null) {
            return HandleResult.TERMINATE;
        }

        String currentRaw = cursor.currentRawIfPresent();
        if (currentRaw != null) {
            return HandleResult.NEXT_HANDLER; // Not empty input, let other handlers process
        }

        try {

            if (currentParameter.isOptional()) {
                handleEmptyOptional(currentParameter, cursor, context);
                cursor.skipParameter();
                return HandleResult.NEXT_ITERATION;
            }

            //required parameter with no input - invalid syntax
            var closestUsage = result.getClosestUsage();
            String invalidUsage = context.imperatConfig().commandPrefix() + context.getRootCommandLabelUsed() + " " + context.arguments().join(" ");
            return HandleResult.failure(new InvalidSyntaxException(invalidUsage, closestUsage));

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
                                                 .provide(context, flagArgument);
                if (defaultStrValue != null) {
                    java.lang.Object flagInputValue = flagInputType.parse(context, flagArgument, defaultStrValue);
                    parsedFlagArgument = ParsedFlagArgument.forDefaultFlag(flagArgument, defaultStrValue, flagInputValue);
                }else {
                    parsedFlagArgument = ParsedFlagArgument.forDefaultFlag(flagArgument, "null", null);
                }
            }
            context.resolveFlag(parsedFlagArgument);

        } else {
            context.parseArgument(
                    new ParsedArgument<>(
                            null,
                            optionalEmptyParameter,
                            stream.position().getParameter(),
                            getDefaultValue(context, optionalEmptyParameter)
                    )
            );
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getDefaultValue(ExecutionContext<S> context, Argument<S> argument) throws CommandException {
        DefaultValueProvider optionalSupplier = argument.getDefaultValueSupplier();
        if (optionalSupplier.isEmpty()) {
            return null;
        }
        String value = optionalSupplier.provide(context, argument);

        if (value != null) {
            return (T) argument.type().parse(context, argument, value);
        }

        return null;
    }
}
