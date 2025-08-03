package studio.mevera.imperat.exception;

import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;

public interface ThrowableResolver<E extends Throwable, S extends Source> {

    void resolve(final E exception, ImperatConfig<S> imperat, Context<S> context);

}
