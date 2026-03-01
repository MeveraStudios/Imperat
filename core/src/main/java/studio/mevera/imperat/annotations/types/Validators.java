package studio.mevera.imperat.annotations.types;

import studio.mevera.imperat.command.parameters.validator.ArgValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for declaring one or more {@link ArgValidator} types on a command type
 * or a specific parameter.
 *
 * <p>This annotation is retained at runtime and can be discovered via reflection.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface Validators {

    /**
     * @return the validator classes to apply
     */
    Class<? extends ArgValidator<?>>[] value();

}
