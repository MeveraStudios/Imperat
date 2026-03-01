package studio.mevera.imperat.command.processors;

import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;

/**
 * Defines a functional interface that processes a {@link CommandContext}
 * AFTER the resolving of the arguments into values.
 *
 * @param <S> the command sender valueType
 */
public interface CommandPostProcessor<S extends Source> extends CommandProcessor<S> {

    /**
     * Processes context AFTER the resolving operation.
     *
     * @param imperat the api
     * @param context the context
     * @throws CommandException the exception to throw if something happens
     */
    void process(
            Imperat<S> imperat,
            ExecutionContext<S> context
    ) throws CommandException;

}
