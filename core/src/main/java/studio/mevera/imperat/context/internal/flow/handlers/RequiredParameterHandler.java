package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.flow.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;

public final class RequiredParameterHandler<S extends Source> implements ParameterHandler<S> {

    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, Cursor<S> stream) throws CommandException {
        Argument<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();

        if (currentParameter == null) {
            return HandleResult.TERMINATE;
        } else if (!currentParameter.isRequired()) {
            return HandleResult.NEXT_HANDLER;
        } else if (currentRaw == null) {
            // Required parameter missing
            var closestUsage = context.getDetectedUsage();
            var exception = new CommandException(ResponseKey.INVALID_SYNTAX);

            if (closestUsage != null) {
                var command = context.getLastUsedCommand();
                if (command != null) {
                    exception.withPlaceholder("closest_usage_line",
                            "Closest Usage: " + context.imperatConfig().commandPrefix() + CommandPathway.format(command, closestUsage));
                } else {
                    exception.withPlaceholder("closest_usage_line", "");
                }
            } else {
                exception.withPlaceholder("closest_usage_line", "");
            }

            return HandleResult.failure(exception);
        }


        try {
            var value = currentParameter.type().parse(context, stream, stream.readInput());

            context.resolveArgument(stream, value);
            stream.skip();

            return HandleResult.NEXT_ITERATION;
        } catch (CommandException e) {
            return HandleResult.failure(e);
        }
    }
}