package studio.mevera.imperat.annotations;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.AttachmentMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents the Fallback attachment mode that will be set for all subcommands that have
 * their attachment mode = `UNSET`.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ApiStatus.AvailableSince("1.9.0")
public @interface GlobalAttachmentMode {

    /**
     * Retrieves the {@link AttachmentMode} associated with this annotation.
     * @return the attachment mode that defines how a subcommand integrates with the usages of a command.
     */
    AttachmentMode value();

}
