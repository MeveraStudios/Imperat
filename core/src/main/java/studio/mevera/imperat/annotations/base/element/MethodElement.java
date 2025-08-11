package studio.mevera.imperat.annotations.base.element;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.context.Source;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class MethodElement extends ParseElement<Method> {

    private final List<ParameterElement> parameters = new ArrayList<>();
    private int inputCount = 0;
    private int optionalCount = 0;
    
    public <S extends Source> MethodElement(
        @NotNull AnnotationParser<S> parser,
        @Nullable ClassElement owningElement,
        @NotNull Method element
    ) {
        super(parser, owningElement, element);
        var params = element.getParameters();
        for (int i = 0; i < params.length; i++) {
            var parameter = params[i];
            //TODO debug this !
            ParameterElement parameterElement = new ParameterElement(parser, owningElement, this, parameter);
            //ImperatDebugger.debug("Adding param '%s' to method '%s'", parameterElement.getName(), this.getName());
            parameters.add(parameterElement);
            if (i > 0 ) {

                if(!parameterElement.isContextResolved()) {
                    inputCount++;
                    if(parameterElement.isOptional()) {
                        optionalCount++;
                    }
                }

            }
        }

    }

    public @Nullable ParameterElement getParameterAt(int index) {
        if (index < 0 || index >= size()) return null;
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
    public @NotNull ParseElement<?> getParent() {
        assert super.getParent() != null;
        return super.getParent();
    }
}
