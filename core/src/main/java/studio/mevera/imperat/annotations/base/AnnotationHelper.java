package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.help.CommandHelp;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.List;

public final class AnnotationHelper {

    public static boolean isMethodHelp(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (TypeUtility.areRelatedTypes(parameter.getParameterizedType(), CommandHelp.class)) {
                return true;
            }
        }
        return false;
    }


    public static <S extends Source> Object[] loadParameterInstances(
        Imperat<S> dispatcher,
        List<CommandParameter<S>> fullParameters,
        S source,
        ExecutionContext<S> context,
        MethodElement method
    ) throws ImperatException {
        
        Object[] paramsInstances = new Object[method.getParameters().size()];

        ParameterElement firstParam = method.getParameterAt(0);
        assert firstParam != null;

        if (dispatcher.canBeSender(firstParam.getType())) {

            if (source.getClass().equals(firstParam.getType())) {
                paramsInstances[0] = source;
            } else {
                paramsInstances[0] = context.getResolvedSource(firstParam.getType());
            }

        } else {
            paramsInstances[0] = context.getResolvedSource(firstParam.getType());
        }

        for (int i = 1, p = 0; i < method.size(); i++, p++) {
            ParameterElement actualParameter = method.getParameterAt(i);
            assert actualParameter != null;

            if(actualParameter.isContextResolved()) {
                var contextResolver = dispatcher.config().getMethodParamContextResolver(actualParameter);

                if (contextResolver != null) {
                    paramsInstances[i] = contextResolver.resolve(context, actualParameter);
                    p--;
                    continue;
                }else {

                    throw new IllegalStateException(
                            "In class '%s', In method '%s', The parameter '%s' is set to be context resolved while not having a context resolver for its type '%s'"
                                    .formatted(method.getParent().getName(), method.getName(), actualParameter.getName(),
                                            actualParameter.getType().getTypeName())
                    );
                }
            }

            CommandParameter<S> parameter = getUsageParam(fullParameters, p);
            if (parameter == null) {
                if (actualParameter.isAnnotationPresent(Flag.class)) {
                    Flag flag = actualParameter.getAnnotation(Flag.class);
                    assert flag != null;
                    if (flag.free()) {
                        paramsInstances[i] = context.getFlagValue(flag.value()[0]);
                    } else {
                        throw new IllegalArgumentException();
                    }

                }
                p--;
                continue;
            }

            String name = parameter.name();

            if (parameter.isFlag()) {
                var flagValue = context.getFlagValue(name);
                if(flagValue == null && parameter.asFlagParameter().isSwitch()) {
                    paramsInstances[i] = false;
                }else {
                    paramsInstances[i] = flagValue;
                }
            } else {
                var ctxArg = context.getArgument(name);
                paramsInstances[i] = ctxArg;
            }

        }
        return paramsInstances;
    }

    private static <S extends Source> @Nullable CommandParameter<S> getUsageParam(List<? extends CommandParameter<S>> params, int index) {
        if (index < 0 || index >= params.size()) return null;
        return params.get(index);
    }

    public static <S extends Source> @NotNull String getParamName(
        ImperatConfig<S> imperat,
        ParameterElement parameter,
        @Nullable Named named,
        @Nullable Flag flag,
        @Nullable Switch switchAnnotation
    ) {
        String name;

        if (named != null)
            name = named.value();
        else if (flag != null)
            name = flag.value()[0];
        else if (switchAnnotation != null)
            name = switchAnnotation.value()[0];
        else
            name = parameter.getElement().getName();

        return imperat.replacePlaceholders(name);
    }

    public static <S extends Source> @NotNull String getParamName(ImperatConfig<S> imperat, ParameterElement parameter) {
        return getParamName(
            imperat,
            parameter,
            parameter.getAnnotation(Named.class),
            parameter.getAnnotation(Flag.class),
            parameter.getAnnotation(Switch.class));
    }

    public static @NotNull OptionalValueSupplier getOptionalValueSupplier(
        Class<? extends OptionalValueSupplier> supplierClass
    ) throws NoSuchMethodException, InstantiationException,
        IllegalAccessException, InvocationTargetException {

        var emptyConstructor = supplierClass.getDeclaredConstructor();
        emptyConstructor.setAccessible(true);

        return emptyConstructor.newInstance();
    }

    public static @NotNull OptionalValueSupplier deduceOptionalValueSupplier(
        ParameterElement parameter,
        Default defaultAnnotation,
        DefaultProvider provider,
        OptionalValueSupplier fallback
    ) throws ImperatException {

        if (defaultAnnotation != null) {
            String def = defaultAnnotation.value();
            return OptionalValueSupplier.of(def);
        } else if (provider != null) {
            Class<? extends OptionalValueSupplier> supplierClass = provider.value();
            try {
                return getOptionalValueSupplier(supplierClass);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new IllegalAccessError("Optional value suppler class '" +
                    supplierClass.getName() + "' doesn't have an empty accessible constructor !");
            }
        }
        return fallback;
    }

    public static boolean isHelpParameter(ParameterElement element) {
        return TypeUtility.areRelatedTypes(element.getType(), CommandHelp.class);
    }


    public static boolean isAbnormalClass(ParseElement<?> parseElement) {
        if(parseElement instanceof ClassElement classElement) {
            return isAbnormalClass(classElement.getElement());
        }
        return false;
    }
    public static boolean isAbnormalClass(Class<?> element) {
        return element.isInterface() || element.isEnum() || Modifier.isAbstract(element.getModifiers());
    }
}
