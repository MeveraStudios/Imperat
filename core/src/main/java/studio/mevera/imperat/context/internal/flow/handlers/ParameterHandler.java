package studio.mevera.imperat.context.internal.flow.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.flow.HandleResult;
import studio.mevera.imperat.exception.CommandException;

public sealed interface ParameterHandler<S extends Source>
        permits EmptyInputHandler,
                        OptionalParameterHandler,
                        RequiredParameterHandler,
                        SubCommandHandler {


    @NotNull HandleResult handle(ExecutionContext<S> context, Cursor<S> stream) throws CommandException;

}