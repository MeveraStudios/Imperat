package studio.mevera.imperat.annotations.parameters;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Represents an annotated parameter
 * from using annotation parser
 */
@ApiStatus.AvailableSince("1.0.0")
public interface AnnotatedParameter<S extends Source> extends CommandParameter<S> {

    /**
     * Get the instance of specific annotation
     *
     * @param clazz the valueType of annotation
     * @param <A>   the valueType of the annotation
     * @return the specific instance of an annotation of a certain valueType {@linkplain A}
     */
    @Nullable
    <A extends Annotation> A getAnnotation(Class<A> clazz);

    default boolean hasAnnotation(Class<? extends Annotation> clazz) {
        return getAnnotation(clazz) != null;
    }

    /**
     * @return the annotations associated with this parameter
     */
    Collection<? extends Annotation> getAnnotations();


}
