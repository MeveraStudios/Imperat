package studio.mevera.imperat.annotations;

import studio.mevera.imperat.command.parameters.type.ArgumentType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the argument type for a parameter/argument.
 * This can be used to specify custom parsing or validation behavior per argument.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface ArgType {

    Class<? extends ArgumentType<?, ?>> value();

}
