package studio.mevera.imperat.exception;

import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.context.Source;

public final class InvalidSyntaxException extends CommandException {

    private final CommandPathSearch<?> result;

    public <S extends Source> InvalidSyntaxException(CommandPathSearch<S> result) {
        super();
        this.result = result;
    }

    public CommandPathSearch<?> getExecutionResult() {
        return result;
    }
}
