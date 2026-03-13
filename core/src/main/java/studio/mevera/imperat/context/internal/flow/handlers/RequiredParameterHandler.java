package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.tree.TreeExecutionResult;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.flow.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSyntaxException;

public final class RequiredParameterHandler<S extends CommandSource> implements ParameterHandler<S> {

    @Override
    public @NotNull HandleResult handle(TreeExecutionResult<S> result, ExecutionContext<S> context, Cursor<S> stream) throws CommandException {
        Argument<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();

        if (currentParameter == null) {
            return HandleResult.TERMINATE;
        } else if (!currentParameter.isRequired()) {
            return HandleResult.NEXT_HANDLER;
        } else if (currentRaw == null) {
            // Required parameter missing
            var closestUsage = context.getDetectedPathway();
            String closestUsageStr = null;
            if (closestUsage != null) {
                var command = context.getLastUsedCommand();
                if (command != null) {
                    closestUsageStr = context.imperatConfig().commandPrefix() + CommandPathway.format(command, closestUsage);
                }
            }

            return HandleResult.failure(new InvalidSyntaxException(
                    context.imperatConfig().commandPrefix() + context.getRootCommandLabelUsed() + " " + context.arguments().join(" "),
                    closestUsageStr
            ));
        }


        try {
            var value = currentParameter.type().parse(context, stream, stream.readInput());

            context.parseArgument(stream, value);
            stream.skip();

            return HandleResult.NEXT_ITERATION;
        } catch (CommandException e) {
            return HandleResult.failure(e);
        }
    }
}