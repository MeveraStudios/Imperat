package studio.mevera.imperat.command.processors;

import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ImperatException;

/**
 * Defines a functional interface that processes a {@link Context}
 * BEFORE the resolving of the arguments into values.
 *
 * @param <S> the command sender valueType
 */
@FunctionalInterface
public interface CommandPreProcessor<S extends Source> extends CommandProcessor<S> {

    /**
     * Processes context BEFORE the resolving operation.
     *
     * @param imperat the api
     * @param context the context
     * @param usage   The usage detected
     * @throws ImperatException the exception to throw if something happens
     */
    void process(
        Imperat<S> imperat,
        Context<S> context,
        CommandUsage<S> usage
    ) throws ImperatException;

}
