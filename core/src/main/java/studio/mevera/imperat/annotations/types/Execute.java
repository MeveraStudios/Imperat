package studio.mevera.imperat.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a command usage method and provides example invocations for help output.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Execute {

    /**
     * Example command usages to display in help output.
     * Each string should represent an invocation with valid input for the command.
     *
     * @return array of example usages
     */
    String[] examples() default {};

}
