package studio.mevera.imperat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one or more flag names for a parameter or type.
 * Flags are optional markers (e.g. -silent, --force). If {@code free} is true,
 * the flag can be present without an explicit value and will toggle to true.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface Flag {

    String[] value();

    boolean free() default false;
}
