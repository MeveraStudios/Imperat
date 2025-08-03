package studio.mevera.imperat.annotations;

import studio.mevera.imperat.command.AttachmentMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     * @return The type of attachment of this subcommand to its parent command.
     */
    AttachmentMode attachment() default AttachmentMode.UNSET;
}
