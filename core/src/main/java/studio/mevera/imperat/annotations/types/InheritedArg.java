package studio.mevera.imperat.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter as inherited from an ancestor command's pathway.
 * The framework automatically resolves which ancestor pathway provides this parameter.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface InheritedArg {

    /**
     * The name of the argument to inherit from ancestor pathway.
     * If empty, the parameter name is used for matching.
     */
    String value() default "";

    /**
     * The expected type of the inherited argument (for validation).
     * If not specified, uses the parameter's actual type.
     */
    Class<?> type() default Void.class;
}