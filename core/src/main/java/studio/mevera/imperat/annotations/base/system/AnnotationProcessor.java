package studio.mevera.imperat.annotations.base.system;

import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.context.Source;

import java.lang.annotation.Annotation;

/**
 * Processor for a specific annotation type.
 * Implementations handle the logic for processing their annotation during command parsing.
 */
public interface AnnotationProcessor<T extends Annotation, S extends Source> {

    /**
     * Processes the annotation in the current parsing context.
     *
     * @param context The current parsing context
     * @param annotation The annotation instance
     * @param element The element being processed (class, method, or parameter)
     */
    void process(ParseContext<S> context, T annotation, ParseElement<?> element);

    /**
     * The annotation type this processor handles.
     */
    Class<T> getAnnotationType();

    /**
     * Priority for processing order. Lower values = earlier processing.
     */
    default int getPriority() {
        return 100;
    }
}