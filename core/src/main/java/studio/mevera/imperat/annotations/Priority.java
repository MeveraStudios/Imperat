package studio.mevera.imperat.annotations;

import java.lang.annotation.*;

/**
 * Defines the execution or evaluation priority of an element.
 * Lower values indicate higher priority (executed first).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface Priority {

    /**
     * The priority value.
     * Lower values represent higher priority.
     * Unannotated elements are treated as having Integer.MAX_VALUE.
     */
    int value();
}
