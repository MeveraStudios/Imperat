package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.ContextArgumentProviderFactory;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.providers.ContextArgumentProvider;

import java.lang.reflect.Type;

public final class ContextArgumentsConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    ContextArgumentsConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public <T> ContextArgumentsConfig<S> provider(Type type, @NotNull ContextArgumentProvider<S, T> provider) {
        config.registerContextArgumentProvider(type, provider);
        return this;
    }

    public <T> ContextArgumentsConfig<S> factory(Type type, @NotNull ContextArgumentProviderFactory<S, T> factory) {
        config.registerContextArgumentProviderFactory(type, factory);
        return this;
    }
}
