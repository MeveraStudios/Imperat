package studio.mevera.imperat.annotations.types;

import studio.mevera.imperat.annotations.base.DefaultExecutorServiceProvider;
import studio.mevera.imperat.annotations.base.ExecutorServiceProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a command handling method to be executed asynchronously.
 * Applies to methods only.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Async {

    /**
     * Optional parameter to specify a custom executor service provider.
     * If not specified, the default executor service will be used.
     *
     * @return The class of the custom executor service provider, or {@link DefaultExecutorServiceProvider} if not specified.
     */
    Class<? extends ExecutorServiceProvider> value() default DefaultExecutorServiceProvider.class;
}
