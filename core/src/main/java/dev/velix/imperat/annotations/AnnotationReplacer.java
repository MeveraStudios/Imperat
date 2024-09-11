package dev.velix.imperat.annotations;

import dev.velix.imperat.annotations.types.Command;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Replaces a custom annotation made by the user, with annotations
 * made from the basic pre-made annotations such as {@link Command}
 *
 * @param <A> the type of annotation to replace with other annotations
 */
@ApiStatus.AvailableSince("1.0.0")
public interface AnnotationReplacer<A extends Annotation> {
    
    /**
     * The annotation to replace
     *
     * @param annotation the annotation
     * @return the annotations replaced by this annotation
     */
    @NotNull
    Collection<Annotation> replace(A annotation);
    
}
