package studio.mevera.imperat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a subcommand on a type or method, with optional attachment mode and
 * suggestion-permission behavior.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SubCommand {

    /**
     * @return The names of subcommands
     */
    String[] value();

    /**
     * @return Whether to skip permission checks during auto-completion.
     */
    boolean skipSuggestionsChecks() default false;

    /**
     * The format of the node/argument to attach this subcommand to. If empty, attaches to the parent command.
     * @return the format of the node/argument to attach this subcommand to, or empty to attach to the parent command
     */
    String attachTo() default "";
}
