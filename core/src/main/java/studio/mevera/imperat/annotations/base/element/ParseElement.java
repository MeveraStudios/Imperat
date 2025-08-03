package studio.mevera.imperat.annotations.base.element;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.AnnotationMap;
import studio.mevera.imperat.util.ImperatDebugger;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public sealed abstract class ParseElement<E extends AnnotatedElement> implements AnnotatedElement, Iterable<Annotation> permits ClassElement, MethodElement, ParameterElement {

    protected final AnnotationParser<?> parser;
    protected final @NotNull AnnotationMap annotations = new AnnotationMap();
    protected final @Nullable ParseElement<?> parent;
    protected final @NotNull E element;

    public <S extends Source> ParseElement(
        @NotNull AnnotationParser<S> parser,
        @Nullable ParseElement<?> parent,
        @NotNull E element
    ) {
        this.parser = parser;
        this.parent = parent;
        this.element = element;
        this.load(parser);
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation, S extends Source> void load(@NotNull AnnotationParser<S> registry) {
        for (Annotation annotation : element.getDeclaredAnnotations()) {
            Class<A> clazz = (Class<A>) annotation.annotationType();
            annotations.put(clazz, annotation);
            if (!registry.isKnownAnnotation(clazz) && registry.hasAnnotationReplacerFor(clazz)) {
                //we add the custom annotation anyway
                ImperatDebugger.debug("Found replacer for '@%s' on element '%s'",clazz.getSimpleName(), this.getName());
                annotations.put(clazz, annotation);

                //adding the replaced annotations
                AnnotationReplacer<A> replacer = registry.getAnnotationReplacer(clazz);
                assert replacer != null;

                Collection<Annotation> replacementAnnotations = replacer.replace(this, (A) annotation);
                for(Annotation replacement : replacementAnnotations) {
                    annotations.put(replacement.annotationType(), replacement);
                }
            }
        }
    }

    /**
     * Returns this element's annotation for the specified valueType if
     * such an annotation is <em>present</em>, else null.
     *
     * @param annotationClass the Class object corresponding to the
     *                        annotation valueType
     * @return this element's annotation for the specified annotation valueType if
     * present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> @Nullable T getAnnotation(@NotNull Class<T> annotationClass) {
        return (T) annotations.get(annotationClass);
    }

    /**
     * Returns annotations that are <em>present</em> on this element.
     * <p>
     * If there are no annotations <em>present</em> on this element, the return
     * value is an array of length 0.
     * <p>
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations present on this element
     * @since 1.5
     */
    @Override
    public Annotation @NotNull [] getAnnotations() {
        return annotations.values().toArray(new Annotation[0]);
    }

    /**
     * Returns annotations that are <em>directly present</em> on this element.
     * This method ignores inherited annotations.
     * <p>
     * If there are no annotations <em>directly present</em> on this element,
     * the return value is an array of length 0.
     * <p>
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations directly present on this element
     * @since 1.5
     */
    @Override
    public Annotation @NotNull [] getDeclaredAnnotations() {
        return annotations.values().toArray(new Annotation[0]);
    }


    /**
     * Returns an iterator over elements of valueType {@code T}.
     *
     * @return an Iterator.
     */
    @NotNull
    @Override
    public Iterator<Annotation> iterator() {
        return annotations.values().iterator();
    }

    @Override
    public String toString() {
        if (element instanceof Method method) {
            return method.getName();
        }
        if (element instanceof Class<?> clazz) {
            return clazz.getSimpleName();
        }
        return element.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParseElement<?> that)) return false;
        return Objects.equals(parent, that.parent)
            && Objects.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, element);
    }

    public abstract String getName();

    public @Nullable ParseElement<?> getParent() {
        return parent;
    }

    @NotNull
    public E getElement() {
        return element;
    }

}
