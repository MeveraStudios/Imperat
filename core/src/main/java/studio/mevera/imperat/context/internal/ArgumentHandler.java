package studio.mevera.imperat.context.internal;

import studio.mevera.imperat.command.tree.TreeExecutionResult;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;

public sealed interface ArgumentHandler<S extends CommandSource>
        permits
        OptionalArgumentHandler {


    void handle(TreeExecutionResult<S> result, ExecutionContext<S> context, Cursor<S> stream) throws CommandException;

}