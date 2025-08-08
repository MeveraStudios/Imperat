package studio.mevera.imperat.exception;

import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;

public final class InvalidSyntaxException extends ImperatException {

    private final CommandPathSearch<?> result;
    
    public <S extends Source> InvalidSyntaxException(CommandPathSearch<S> result, Context<?> ctx) {
        super(ctx);
        this.result = result;
    }
    
    public CommandPathSearch<?> getExecutionResult() {
        return result;
    }
}
