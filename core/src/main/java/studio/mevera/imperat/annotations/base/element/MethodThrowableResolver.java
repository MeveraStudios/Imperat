package studio.mevera.imperat.annotations.base.element;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.util.asm.MethodCaller;

public final class MethodThrowableResolver<E extends Throwable, S extends Source> implements ThrowableResolver<E, S> {
    
    private final MethodCaller.BoundMethodCaller caller;
    private final Class<E> exceptionType;
    
    MethodThrowableResolver(MethodCaller.BoundMethodCaller caller, Class<E> exceptionType) {
        this.caller = caller;
        this.exceptionType = exceptionType;
    }
    
    public Class<E> getExceptionType() {
        return exceptionType;
    }
    
    @Override
    public void resolve(E exception, Context<S> context) {
        caller.call(exception, context);
    }
}
