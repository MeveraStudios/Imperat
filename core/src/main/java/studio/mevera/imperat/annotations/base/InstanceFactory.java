package studio.mevera.imperat.annotations.base;


import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.UnknownDependencyException;

/**
 * Functional interface for creating instances of a given class.
 */
public abstract class InstanceFactory<S extends Source> {
    
    protected final ImperatConfig<S> config;
    
    protected InstanceFactory(ImperatConfig<S> config) {
        this.config = config;
    }
    
    /**
     * Creates an instance of the specified class.
     *
     * @param cls the class to instantiate
     * @param <T> the type of the instance
     * @return a new instance of the specified class
     */
    public abstract <T> @NotNull T createInstance(Class<T> cls) throws UnknownDependencyException;
    
    /**
     * Creates a default instance factory that uses the provided ImperatConfig for dependency resolution.
     *
     * @param config the ImperatConfig to use for resolving dependencies
     * @param <S>    the type of Source
     * @return a default InstanceFactory
     */
    public static <S extends Source> InstanceFactory<S> defaultFactory(ImperatConfig<S> config) {
        return new DefaultInstanceFactory<>(config);
    }
}
