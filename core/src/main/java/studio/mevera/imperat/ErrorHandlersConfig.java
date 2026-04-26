package studio.mevera.imperat;

import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandExceptionHandler;

public final class ErrorHandlersConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    ErrorHandlersConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public <T extends Throwable> ErrorHandlersConfig<S> register(Class<T> exception, CommandExceptionHandler<T, S> handler) {
        config.setErrorHandler(exception, handler);
        return this;
    }
}
