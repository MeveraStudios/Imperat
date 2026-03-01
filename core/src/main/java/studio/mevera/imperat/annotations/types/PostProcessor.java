package studio.mevera.imperat.annotations.types;

import studio.mevera.imperat.command.processors.CommandPostProcessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers one or more {@link CommandPostProcessor} implementations to run
 * after a command method executes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PostProcessor {

    Class<? extends CommandPostProcessor<?>>[] value();

}
