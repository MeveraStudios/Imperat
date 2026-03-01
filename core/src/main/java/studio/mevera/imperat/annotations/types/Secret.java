package studio.mevera.imperat.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a command or command class/method as secret, meaning it will not be listed in help commands
 * and may be hidden from tab-completion results. Applies to both classes and methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secret {

    // MARKER INTERFACE - NO FIELDS OR METHODS NEEDED

}
