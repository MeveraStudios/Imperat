package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.sur.HandleResult;

public sealed interface ParameterHandler<S extends Source>
        permits CommandParameterHandler, EmptyInputHandler, FlagInputHandler,
        FreeFlagHandler, NonFlagWhenExpectingFlagHandler,
        OptionalParameterHandler, RequiredParameterHandler
{
    
    @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream);
    
}