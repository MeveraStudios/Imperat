package studio.mevera.imperat.annotations.base.element.selector;

import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.context.Source;

import java.lang.reflect.Modifier;
import java.util.Arrays;

public interface MethodRules {

    Rule<MethodElement> IS_PUBLIC = Rule.buildForMethod()
        .condition((imperat, registry, method) -> Modifier.isPublic(method.getModifiers()))
        .failure((registry, method) -> {
            throw methodError(method, "is not public");
        })
        .build();

    Rule<MethodElement> HAS_KNOWN_SENDER = Rule.buildForMethod()
        .condition((imperat, registry, method) -> {
            ParameterElement parameterElement = method.getParameterAt(0);
            if (parameterElement == null) return false;
            return imperat.canBeSender(parameterElement.getType())
                || imperat.config().hasSourceResolver(parameterElement.getType());
        })
        .failure((registry, method) -> {
            ParameterElement parameterElement = method.getParameterAt(0);
            String msg;
            if (parameterElement == null) {
                msg = "Method '" + method.getName() + "' has no parameters";
            } else {
                msg = "First parameter of valueType '" + parameterElement.getType().getTypeName() + "' is not a sub-valueType of `" + Source.class.getName() + "'";
            }
            throw methodError(method, msg);
        })
        .build();

    Rule<MethodElement> HAS_A_MAIN_ANNOTATION = Rule.buildForMethod()
        .condition((imperat, registry, element) -> {
            long count = Arrays.stream(element.getDeclaredAnnotations())
                .filter(annotation -> registry.isEntryPointAnnotation(annotation.annotationType())).count();
            return count > 0;
        })
        .failure((registry, element) -> {
            throw methodError(element, "doesn't have any main annotations!");
        })
        .build();

    private static IllegalStateException methodError(MethodElement element, String msg) {
        ClassElement parent = (ClassElement) element.getParent();

        return new IllegalStateException(
            String.format("Method '%s' In class '%s' " + msg,
                element.getElement().getName(), parent.getElement().getName()
            )
        );
    }
}
