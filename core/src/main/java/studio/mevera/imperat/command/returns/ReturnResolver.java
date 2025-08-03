package studio.mevera.imperat.command.returns;

import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.lang.reflect.Type;

public interface ReturnResolver<S extends Source, T> {

    /**
     * Handles the return value of a command.
     *
     * @param context The execution contex of the command.
     * @param method  The method element of the command.
     * @param value   The return value of the command.
     */
    void handle(ExecutionContext<S> context, MethodElement method, T value);

    /**
     * Returns the type of the return value.
     *
     * @return The type of the return value.
     */
    Type getType();

}
