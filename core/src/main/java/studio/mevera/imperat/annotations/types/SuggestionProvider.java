package studio.mevera.imperat.annotations.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a parameter's suggestions to a named provider registered in the system.
 * Use when suggestions are dynamic or context-sensitive.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SuggestionProvider {

    Class<? extends studio.mevera.imperat.providers.SuggestionProvider<?>> value();

}
