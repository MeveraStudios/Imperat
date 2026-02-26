package studio.mevera.imperat.annotations.base.system;

import studio.mevera.imperat.context.Source;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of annotation processors for extensible command parsing.
 */
public final class AnnotationHandlerRegistry<S extends Source> {

    private final Map<Class<? extends Annotation>, AnnotationProcessor<?, S>> processors = new HashMap<>();

    public <T extends Annotation> void register(AnnotationProcessor<T, S> processor) {
        processors.put(processor.getAnnotationType(), processor);
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> AnnotationProcessor<T, S> getProcessor(Class<T> annotationType) {
        return (AnnotationProcessor<T, S>) processors.get(annotationType);
    }

    public boolean hasProcessor(Class<? extends Annotation> annotationType) {
        return processors.containsKey(annotationType);
    }

    /**
     * Gets all registered processors sorted by priority
     */
    public java.util.List<AnnotationProcessor<?, S>> getSortedProcessors() {
        return processors.values().stream()
                       .sorted(Comparator.comparingInt(AnnotationProcessor::getPriority))
                       .toList();
    }
}