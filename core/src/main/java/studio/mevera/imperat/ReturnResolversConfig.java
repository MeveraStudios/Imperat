package studio.mevera.imperat;

import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.context.CommandSource;

import java.lang.reflect.Type;

public final class ReturnResolversConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    ReturnResolversConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public ReturnResolversConfig<S> register(Type type, ReturnResolver<S, ?> returnResolver) {
        if (!returnResolver.getType().equals(type)) {
            throw new IllegalArgumentException("The return resolver entered, has a to-return type that does not match the entered type.");
        }
        config.registerReturnResolver(type, returnResolver);
        return this;
    }
}
