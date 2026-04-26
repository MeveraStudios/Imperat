package studio.mevera.imperat;

import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.providers.DependencySupplier;

import java.lang.reflect.Type;

public final class DependenciesConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    DependenciesConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public DependenciesConfig<S> register(Type type, DependencySupplier resolver) {
        config.registerDependencyResolver(type, resolver);
        return this;
    }
}
