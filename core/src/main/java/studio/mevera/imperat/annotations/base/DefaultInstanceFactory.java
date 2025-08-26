package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.UnknownDependencyException;

final class DefaultInstanceFactory<S extends Source> implements InstanceFactory<S> {
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull T createInstance(ImperatConfig<S> config, Class<T> cls) throws UnknownDependencyException {
        var dependencyResolved = config.resolveDependency(cls);
        if (dependencyResolved != null) {
            return (T) dependencyResolved;
        }
        
        //try instatiating it from an empty constructor
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new UnknownDependencyException("Failed to create instance of " + cls.getName(), e);
        }
    }
    
}
