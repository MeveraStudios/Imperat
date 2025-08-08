package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;

@ApiStatus.AvailableSince("1.0.0")
public abstract class SelfHandledException extends ImperatException {
    
    public SelfHandledException(Context<?> ctx) {
        super(ctx);
    }
    
    /**
     * Handles the exception
     *
     * @param <S>     the command-source valueType
     * @param imperat the api
     * @param context the context
     */
    public abstract <S extends Source> void handle(ImperatConfig<S> imperat, Context<S> context);

}
