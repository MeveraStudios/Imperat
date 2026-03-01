package studio.mevera.imperat.annotations.types;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or parameter whose value is resolved from the execution context
 * rather than parsed from the command input.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
@ApiStatus.AvailableSince("1.9.0")
public @interface Context {

}
