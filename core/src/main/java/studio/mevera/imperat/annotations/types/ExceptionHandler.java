package studio.mevera.imperat.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method as a handler for a specific {@link Throwable} type.
 * The annotated method will be invoked when the given exception is thrown.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionHandler {

    Class<? extends Throwable> value();

}
