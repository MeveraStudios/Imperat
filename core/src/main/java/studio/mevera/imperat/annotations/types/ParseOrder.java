package studio.mevera.imperat.annotations.types;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the execution or evaluation priority of an element.
 * Lower values indicate higher priority (executed first).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface ParseOrder {

    /**
     * The priority value.
     * Lower values represent higher priority.
     * Unannotated elements are treated as having Integer.MAX_VALUE.
     */
    int value();
}
