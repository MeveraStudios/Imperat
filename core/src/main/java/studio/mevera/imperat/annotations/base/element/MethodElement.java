package studio.mevera.imperat.annotations.base.element;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.types.ExplicitReturnResolver;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.context.CommandSource;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class MethodElement extends ParseElement<Method> {

    private final List<ParameterElement> parameters = new ArrayList<>();
    private int inputCount = 0;
    private int optionalCount = 0;
    private final ReturnResolver<?, ?> returnResolver;
    public <S extends CommandSource> MethodElement(
            @NotNull AnnotationParser<S> parser,
            @Nullable ClassElement owningElement,
            @NotNull Method element,
            @Nullable ReturnResolver<?, ?> returnResolver
    ) {
        super(parser, owningElement, element);
        this.returnResolver = returnResolver;
        var params = element.getParameters();
        for (int i = 0; i < params.length; i++) {
            var parameter = params[i];
            ParameterElement parameterElement = new ParameterElement(parser, owningElement, this, parameter);
            //ImperatDebugger.debug("Adding param '%s' to method '%s'", parameterElement.getName(), this.getName());
            parameters.add(parameterElement);
            if (i > 0) {

                if (!parameterElement.isContextResolved()) {
                    inputCount++;
                    if (parameterElement.isOptional()) {
                        optionalCount++;
                    }
                }

            }
        }

    }

    public <S extends CommandSource> MethodElement(
            @NotNull AnnotationParser<S> parser,
            @Nullable ClassElement owningElement,
            @NotNull Method element
    ) {
        this(parser, owningElement, element, deduceReturnResolver(parser, element));
    }

    private static <S extends CommandSource> @Nullable ReturnResolver<?, ?> deduceReturnResolver(AnnotationParser<S> parser, Method method) {
        var annotation = method.getAnnotation(ExplicitReturnResolver.class);
        if (annotation != null) {
            if (method.getReturnType() == void.class) {
                throw new IllegalStateException(
                        "Method '%s' is annotated with @ExplicitReturnResolver but has a void return type".formatted(method.getName()));
            }
            try {
                var cfg = parser.getImperat().config();
                return cfg.getInstanceFactory().createInstance(cfg, annotation.value());
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to instantiate return resolver '%s' for method '%s'".formatted(annotation.value().getTypeName(), method.getName()),
                        e);
            }
        }
        return null;
    }

    public ReturnResolver<?, ?> getSpecificReturnResolver() {
        return returnResolver;
    }

    public @Nullable ParameterElement getParameterAt(int index) {
        if (index < 0 || index >= size()) {
            return null;
        }
        return parameters.get(index);
    }


    public int size() {
        return parameters.size();
    }

    public Type getReturnType() {
        return getElement().getReturnType();
    }

    public int getModifiers() {
        return getElement().getModifiers();
    }

    @Override
    public String getName() {
        return getElement().getName();
    }

    public List<ParameterElement> getParameters() {
        return parameters;
    }

    public int getInputCount() {
        return inputCount;
    }

    public boolean isAllOptionalInput() {
        return inputCount == optionalCount;
    }

    @Override
    public @NotNull ClassElement getParent() {
        assert super.getParent() != null;
        return (ClassElement) super.getParent();
    }

}
