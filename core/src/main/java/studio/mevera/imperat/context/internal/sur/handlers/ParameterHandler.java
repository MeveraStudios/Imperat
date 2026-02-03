package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.CommandException;

public sealed interface ParameterHandler<S extends Source>
        permits EmptyInputHandler, FlagHandler,
                        NonFlagWhenExpectingFlagHandler,
                        OptionalParameterHandler,
                        RequiredParameterHandler,
                        SubCommandHandler
{


    @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) throws CommandException;
    
}