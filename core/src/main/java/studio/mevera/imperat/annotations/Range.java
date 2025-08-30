package studio.mevera.imperat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constrains a numeric parameter or type to a minimum/maximum value.
 * Values outside the bounds should be rejected by the parser/validator.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface Range {

    double min() default Double.MIN_VALUE;

    double max() default Double.MAX_VALUE;

}
