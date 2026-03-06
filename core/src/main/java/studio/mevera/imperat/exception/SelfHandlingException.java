package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.Source;

@ApiStatus.AvailableSince("1.0.0")
public abstract class SelfHandlingException extends CommandException {

    public SelfHandlingException() {
        super();
    }

    /**
     * Handles the exception
     *
     * @param <S>     the command-source valueType
     * @param context the context
     */
    public abstract <S extends Source> void handle(CommandContext<S> context);

}

