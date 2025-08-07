package studio.mevera.imperat.command.returns;

import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.lang.reflect.Type;

/**
 * An interface that represents a way to handle a returned value from a method
 * that does is NOT void. (doesn't return void).
 * This is useful when you want your annotated command methods to return values and
 * those values should be handled in a certain way when returned (after the method's invocation).
 *
 * @param <S> the source type
 * @param <T> the type of value to return
 */
public interface ReturnResolver<S extends Source, T> {

    /**
     * Handles the return value of a command.
     *
     * @param context The execution context of the command.
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
