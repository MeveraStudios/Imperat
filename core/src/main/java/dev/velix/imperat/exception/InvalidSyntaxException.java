package dev.velix.imperat.exception;

import dev.velix.imperat.command.tree.CommandPathSearch;
import dev.velix.imperat.context.Source;

public final class InvalidSyntaxException extends ImperatException {

    private final CommandPathSearch<?> result;
    
    public <S extends Source> InvalidSyntaxException(CommandPathSearch<S> result) {
        this.result = result;
    }
    
    public CommandPathSearch<?> getExecutionResult() {
        return result;
    }
}
