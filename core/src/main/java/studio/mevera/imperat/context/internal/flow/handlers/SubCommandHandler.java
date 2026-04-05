package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.tree.TreeExecutionResult;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.flow.HandleResult;
import studio.mevera.imperat.exception.CommandException;

public final class SubCommandHandler<S extends CommandSource> implements ParameterHandler<S> {

    @Override
    public @NotNull HandleResult handle(TreeExecutionResult<S> result, ExecutionContext<S> context, Cursor<S> cursor) throws CommandException {
        Argument<S> currentParameter = cursor.currentParameterIfPresent();
        String currentRaw = cursor.currentRawIfPresent();
        if (currentParameter == null) {
            return HandleResult.TERMINATE;
        } else if (currentRaw == null || !currentParameter.isCommand()) {
            return HandleResult.NEXT_HANDLER;
        }

        try {
            Command<S> parameterSubCmd = (Command<S>) currentParameter;
            if (parameterSubCmd.hasName(currentRaw)) {
                cursor.skip();
                return HandleResult.NEXT_ITERATION;
            } else {
                return HandleResult.failure(new CommandException("Invalid sub-command: '" + currentRaw + "'"));
            }
        } catch (Exception e) {
            return HandleResult.failure(new CommandException("Error processing command parameter", e));
        }
    }
}