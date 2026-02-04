package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.parse.UnknownSubCommandException;

public final class SubCommandHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) throws CommandException {
        CommandParameter<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();
        if(currentParameter == null) {
            return HandleResult.TERMINATE;
        }
        else if(currentRaw == null || !currentParameter.isCommand()) {
            return HandleResult.NEXT_HANDLER;
        }
        
        try {
            Command<S> parameterSubCmd = (Command<S>) currentParameter;
            if (parameterSubCmd.hasName(currentRaw)) {

                //context.setLastCommand(parameterSubCmd);
                stream.skip();
                return HandleResult.NEXT_ITERATION;
            } else {
                return HandleResult.failure(new UnknownSubCommandException(currentRaw));
            }
        } catch (Exception e) {
            return HandleResult.failure(new CommandException("Error processing command parameter", e));
        }
    }
}