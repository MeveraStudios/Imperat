package studio.mevera.imperat.annotations;

import studio.mevera.imperat.command.processors.CommandPreProcessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers one or more {@link CommandPreProcessor} implementations to run
 * before a command method executes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PreProcessor {

    Class<? extends CommandPreProcessor<?>>[] value();

}
