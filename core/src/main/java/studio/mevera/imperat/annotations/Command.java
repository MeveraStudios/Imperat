package studio.mevera.imperat.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Command {

    /**
     * @return The names of this command
     * The first element is the unique name of the command
     * others are going to be considered the aliases
     */
    @NotNull String[] value();

    /**
     * @return Whether to ignore the permission checks
     * while auto-completing/suggesting or not
     */
    boolean skipSuggestionsChecks() default false;

}
