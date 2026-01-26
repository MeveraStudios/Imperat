package studio.mevera.imperat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated command or command container requires explicit user
 * confirmation before it is executed.
 * <p>
 * This annotation can be applied to:
 * <ul>
 *   <li>Types (e.g. classes that define commands)</li>
 *   <li>Methods (e.g. individual command handlers)</li>
 * </ul>
 * When present, the command execution infrastructure should ensure that the user
 * confirms the action before running the annotated command.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireConfirmation {


}
