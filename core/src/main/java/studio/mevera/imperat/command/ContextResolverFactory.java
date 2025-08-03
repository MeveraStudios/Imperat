package studio.mevera.imperat.command;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.resolvers.ContextResolver;

import java.lang.reflect.Type;

/**
 * Represents a context resolver factory
 * that is responsible for creating {@link ContextResolver}
 *
 * @param <S> the command-sender valueType
 */
public interface ContextResolverFactory<S extends Source, T> {

    /**
     * Creates a context resolver based on the parameter
     *
     * @param parameter the parameter (null if used classic way)
     * @return the {@link ContextResolver} specific for that parameter
     */
    @Nullable
    ContextResolver<S, T> create(Type type, @Nullable ParameterElement parameter);

}
