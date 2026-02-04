package studio.mevera.imperat.annotations;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to forward or redirect the usages of a command/subcommand
 * to another command/subcommand, therefore, the forwarded command/subcommand
 * will be executed the same with same input-parameters as the inheriting one.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ApiStatus.Experimental
public @interface Forward {

    String value();
}
