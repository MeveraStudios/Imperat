package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.ExtractedFlagArgument;
import studio.mevera.imperat.context.internal.flow.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSyntaxException;

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
            return HandleResult.failure(
                    new InvalidSyntaxException(context.getPathwaySearch())
            );
        }


        try {
            var value = currentParameter.type().resolve(context, stream, stream.readInput());

            if (value instanceof ExtractedFlagArgument extractedFlagArgument) {
                context.resolveFlag(extractedFlagArgument);
                stream.skip();
            } else {
                context.resolveArgument(context.getLastUsedCommand(), currentRaw, stream.currentParameterPosition(), currentParameter, value);
                stream.skip();
            }

            return HandleResult.NEXT_ITERATION;
        } catch (CommandException e) {
            return HandleResult.failure(e);
        }
    }
}