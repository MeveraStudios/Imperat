package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.annotations.types.RootCommand;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Replaces a custom annotation made by the user, with annotations
 * made from the basic pre-made annotations such as {@link RootCommand}
 *
 * @param <A> the valueType of annotation to replace with other annotations
 */
@ApiStatus.AvailableSince("1.0.0")
public interface AnnotationReplacer<A extends Annotation> {

    /**
     * The annotation to replace
     *
     * @param element the loaded element holding this annotation
     * @param annotation the annotation
     * @return the annotations replaced by this annotation
     */
    @NotNull
    Collection<Annotation> replace(@NotNull ParseElement<?> element, A annotation);

}
