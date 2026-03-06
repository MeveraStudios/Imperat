package studio.mevera.imperat.annotations.base.parsers;

import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandExceptionHandler;
import studio.mevera.imperat.util.asm.MethodCaller;

public final class MethodCommandExceptionHandler<E extends Throwable, S extends Source> implements CommandExceptionHandler<E, S> {

    private final MethodCaller.BoundMethodCaller caller;
    private final Class<E> exceptionType;

    MethodCommandExceptionHandler(MethodCaller.BoundMethodCaller caller, Class<E> exceptionType) {
        this.caller = caller;
        this.exceptionType = exceptionType;
    }

    public Class<E> getExceptionType() {
        return exceptionType;
    }

    @Override
    public void resolve(E exception, CommandContext<S> context) {
        caller.call(exception, context);
    }
}
