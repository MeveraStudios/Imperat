package studio.mevera.imperat.annotations.types;

import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Marks a method as a {@link studio.mevera.imperat.command.processors.CommandProcessor}, which is a method that is executed after the command is
 * executed, regardless of the outcome.
 * The method must have a single parameter of type {@link studio.mevera.imperat.context.CommandContext}.
 * If the parameter was of type {@link studio.mevera.imperat.context.ExecutionContext}, then the processor would be considered an
 * {@link CommandPostProcessor} and would be executed after the processing of the command pathway, otherwise it would be considered a
 * {@link studio.mevera.imperat.command.processors.CommandPreProcessor} and would be executed before the processing of the command pathway.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Processor {

    int priority() default Priority.NORMAL_VALUE;

}
