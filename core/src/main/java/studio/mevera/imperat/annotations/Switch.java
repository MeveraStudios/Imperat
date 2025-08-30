package studio.mevera.imperat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one or more switch names for a boolean-like parameter.
 * If {@code free} is true, presence of the switch toggles the value without
 * requiring an explicit argument.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Switch {

    String[] value();

    boolean free() default false;
}
