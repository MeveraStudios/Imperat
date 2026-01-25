package studio.mevera.imperat.context.internal.sur;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.exception.CommandException;

/**
 * Represents the result of a parameter handler execution,
 * controlling chain flow with results per handler per iteration.
 */
public enum HandleResult {
    /** Terminates the handler chain execution */
    TERMINATE,
    
    /** Continues to the next iteration of the main loop */
    NEXT_ITERATION,
    
    /** Proceeds to the next handler in the chain */
    NEXT_HANDLER,
    
    /** Indicates an error occurred during handling */
    FAILURE;
    
    private CommandException exception;
    
    /**
     * Creates a failure result with the specified exception.
     *
     * @param exception the exception that caused the failure
     * @return a FAILURE result containing the exception
     */
    public static HandleResult failure(CommandException exception) {
        HandleResult result = FAILURE;
        result.exception = exception;
        return result;
    }
    
    /**
     * Gets the exception associated with this result.
     *
     * @return the exception if this is a FAILURE result, null otherwise
     */
    @Nullable
    public CommandException getException() {
        return exception;
    }
}