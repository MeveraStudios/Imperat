package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
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
    public @NotNull HandleResult handle(TreeExecutionResult<S> result, ExecutionContext<S> context, Cursor<S> cursor) throws CommandException {
        Argument<S> currentParameter = cursor.currentParameterIfPresent();
        String currentRaw = cursor.currentRawIfPresent();

        if (currentParameter == null) {
            return HandleResult.TERMINATE;
        } else if (!currentParameter.isRequired()) {
            return HandleResult.NEXT_HANDLER;
        } else if (currentRaw == null) {
            // Required parameter missing
            var closestUsage = context.getDetectedPathway();

            return HandleResult.failure(new InvalidSyntaxException(
                    context.imperatConfig().commandPrefix() + context.getRootCommandLabelUsed() + " " + context.arguments().join(" "),
                    closestUsage
            ));
        }


        try {
            var value = currentParameter.type().parse(context, cursor);

            context.parseArgument(cursor, value);
            cursor.skip();

            return HandleResult.NEXT_ITERATION;
        } catch (CommandException e) {
            return HandleResult.failure(e);
        }
    }
}