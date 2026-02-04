package studio.mevera.imperat.annotations.base;


import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.UnknownDependencyException;

/**
 * Functional interface for creating instances of a given class.
 */
public interface InstanceFactory<S extends Source> {

    /**
     * Creates a default instance factory that uses the provided ImperatConfig for dependency resolution.
     *
     * @param <S>    the type of Source
     * @return a default InstanceFactory
     */
    static <S extends Source> InstanceFactory<S> defaultFactory() {
        return new DefaultInstanceFactory<>();
    }

    /**
     * Creates an instance of the specified class.
     *
     * @param <T>    the type of the instance
     * @param config the config to use for dependency resolution
     * @param cls    the class to instantiate
     * @return a new instance of the specified class
     */
    <T> @NotNull T createInstance(ImperatConfig<S> config, Class<T> cls) throws UnknownDependencyException;
}
