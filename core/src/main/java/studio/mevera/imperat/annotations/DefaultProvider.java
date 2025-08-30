package studio.mevera.imperat.annotations;

import studio.mevera.imperat.command.parameters.OptionalValueSupplier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an external class that provides/supplies a default value for a parameter
 * when the user omits an input.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface DefaultProvider {

    Class<? extends OptionalValueSupplier> value();
}
