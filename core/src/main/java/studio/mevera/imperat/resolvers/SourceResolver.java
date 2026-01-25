package studio.mevera.imperat.resolvers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;

/**
 * An interface whose single responsibility is to resolve {@link S}
 * into {@link R} to allow custom command sources
 *
 * @param <S> the default platform source
 * @param <R> the resulting source
 */
public interface SourceResolver<S extends Source, R> {

    /**
     * Resolves {@link S} into {@link R}
     *
     * @param source the default source within the platform
     * @param ctx
     * @return the resolved source
     */
    @NotNull
    R resolve(S source, Context<S> ctx) throws CommandException;

}
