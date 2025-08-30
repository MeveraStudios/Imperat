package studio.mevera.imperat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares external subcommand classes that should be attached to the annotated
 * command container. Useful to organize subcommands across multiple types.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExternalSubCommand {

    /**
     * @return the children subcommands
     */
    Class<?>[] value();

}
