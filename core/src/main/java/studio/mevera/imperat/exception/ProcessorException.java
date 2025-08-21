package studio.mevera.imperat.exception;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Context;

public final class ProcessorException extends ImperatException {
    
    @Nullable
    private final Command<?> owningCommand;
    private final Type processorType;
    
    public ProcessorException(
            Type type,
            @Nullable Command<?> owningCommand,
            Throwable cause,
            Context<?> ctx
    ) {
        super((owningCommand == null ? "A Global" : "Preprocessor of command '" + owningCommand.name() +"'"), cause, ctx);
        this.processorType = type;
        this.owningCommand = owningCommand;
    }
    
    public boolean isGlobal() {
        return owningCommand == null;
    }
    
    /**
     * @return The command owning the processor that caused this ProcessorException to be thrown.
     * if it's a global processor, it will return null.
     */
    public @Nullable Command<?> getOwningCommand() {
        return owningCommand;
    }
    
    public Type getProcessorType() {
        return processorType;
    }
    
    public enum Type {
        PRE,
        POST;
    }
}
