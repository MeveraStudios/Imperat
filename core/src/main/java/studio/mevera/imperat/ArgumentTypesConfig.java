package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypeHandler;
import studio.mevera.imperat.context.CommandSource;

import java.lang.reflect.Type;

public final class ArgumentTypesConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    ArgumentTypesConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public <T> ArgumentTypesConfig<S> register(Type type, @NotNull ArgumentType<S, T> resolver) {
        config.registerArgType(type, resolver);
        return this;
    }

    public ArgumentTypesConfig<S> handler(@NotNull ArgumentTypeHandler<S> handler) {
        config.registerArgTypeHandler(handler);
        return this;
    }
}
