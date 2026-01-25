package studio.mevera.imperat.command;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;

/**
 * This class represents the execution/action of this command that's triggered when
 * the sender asks for this command to be executed.
 *
 * @param <S> the command sender valueType
 */
@ApiStatus.AvailableSince("1.0.0")
public interface CommandExecution<S extends Source> {

    static <S extends Source> CommandExecution<S> empty() {
        return (source, context) -> {
        };
    }

    /**
     * Executes the command's actions
     *
     * @param source  the source/sender of this command
     * @param context the context of the command
     */
    void execute(final S source, final ExecutionContext<S> context) throws CommandException;

}
