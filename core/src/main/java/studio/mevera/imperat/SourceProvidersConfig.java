package studio.mevera.imperat;

import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.providers.SourceProvider;

import java.lang.reflect.Type;

public final class SourceProvidersConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    SourceProvidersConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public <R> SourceProvidersConfig<S> register(Type type, SourceProvider<S, R> sourceProvider) {
        config.registerSourceProvider(type, sourceProvider);
        return this;
    }
}
