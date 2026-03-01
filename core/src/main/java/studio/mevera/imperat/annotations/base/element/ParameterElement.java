package studio.mevera.imperat.annotations.base.element;

import studio.mevera.imperat.annotations.Context;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.base.AnnotationHelper;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Objects;

public final class ParameterElement extends ParseElement<Parameter> {

    private final String name;
    private final Type type;
    private final ClassElement owningClass;
    private final boolean contextResolved;

    <S extends Source> ParameterElement(
            final AnnotationParser<S> parser,
            final ClassElement owningClass,
            final MethodElement method,
            final Parameter element
    ) {
        super(parser, method, element);
        this.owningClass = owningClass;
        this.name = AnnotationHelper.getParamName(parser.getImperat().config(), this);
        this.type = element.getParameterizedType();
        this.contextResolved = calculateIsContextResolved();
    }

    @Override
    public String toString() {
        return getElement().getType().getSimpleName() + " " + name;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public ClassElement getOwningClass() {
        return owningClass;
    }

    public boolean isOptional() {
        return isAnnotationPresent(Optional.class)
                       || isAnnotationPresent(Default.class)
                       || isAnnotationPresent(DefaultProvider.class)
                       || isAnnotationPresent(Flag.class)
                       || isAnnotationPresent(Switch.class);
    }

    public boolean isContextResolved() {
        return contextResolved;
    }

    private boolean calculateIsContextResolved() {
        // Check if the parameter itself is annotated with @CommandContext
        if (isAnnotationPresent(Context.class)) {
            return true;
        }

        // Check if the Type of the parameter is annotated with @CommandContext
        if (TypeWrap.of(type).getRawType().isAnnotationPresent(Context.class)) {
            return true;
        }

        // Check if any of the annotations on the parameter are annotated with @CommandContext
        for (final Annotation annotation : getDeclaredAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Context.class)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParameterElement that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, type);
    }
}
