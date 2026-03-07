package studio.mevera.imperat.command.processors;

import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

/**
 * Defines a functional interface that processes a {@link CommandContext}
 * BEFORE the resolving of the arguments into values.
 *
 * @param <S> the command sender valueType
 */
@FunctionalInterface
public interface CommandPreProcessor<S extends CommandSource> extends CommandProcessor {

    /**
     * Processes context BEFORE the resolving operation.
     *
     * @param context the context
     * @throws CommandException the exception to throw if something happens
     */
    void process(
            CommandContext<S> context
    ) throws CommandException;

}
