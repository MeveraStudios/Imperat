package studio.mevera.imperat.exception;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;

public final class ProcessorException extends ImperatException {
    
    @Nullable
    private final Command<?> owningCommand;
    public ProcessorException(
            Type type,
            @Nullable Command<?> owningCommand,
            Throwable cause
    ) {
        super((owningCommand == null ? "A Global" : "Preprocessor of command '" + owningCommand.name() +"'"), cause);
        this.owningCommand = owningCommand;
    }
    
    public boolean isGlobal() {
        return owningCommand == null;
    }
    
    public @Nullable Command<?> getOwningCommand() {
        return owningCommand;
    }
    
    public enum Type {
        PRE,
        POST;
    }
}
