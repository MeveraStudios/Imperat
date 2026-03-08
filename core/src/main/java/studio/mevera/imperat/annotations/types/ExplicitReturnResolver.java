package studio.mevera.imperat.annotations.types;

import studio.mevera.imperat.command.returns.ReturnResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a return value resolver for a command.
 * This method will be called to resolve the return value of a command execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExplicitReturnResolver {

    /**
     * The class of the return resolver to use for this method.
     * This should be a class that implements the {@link ReturnResolver} interface.
     *
     * @return the class of the return resolver
     */
    Class<? extends ReturnResolver<?, ?>> value();
}
