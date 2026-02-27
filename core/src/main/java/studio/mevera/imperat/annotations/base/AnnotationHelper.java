package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;

public final class AnnotationHelper {
    /**
     * The function loads the full parameters of a method and resolves all context, such as senders, flags, regular arguments, etc.
     * <p>
     * Checks arguments and resolves their context, if the parameter or type contains ContextResolved then it will get the context resolver from the imperat's config instance.
     * @param dispatcher the imperat instance
     * @param source the source of the command
     * @param context the execution context for a command, responsible for resolving and managing command arguments, flags, and their values during command execution.
     * @param method the method we're operating on
     * @return the array of parameter instances after loading and context resolving
     * @param <S> the source
     * @throws CommandException if resolution fails
     */
    public static <S extends Source> Object[] loadParameterInstances(
            Imperat<S> dispatcher,
            S source,
            ExecutionContext<S> context,
            MethodElement method
    ) throws CommandException {
        if (method.getParameters().isEmpty()) {
            throw new IllegalArgumentException("Method has no parameters");
        }

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

            if (actualParameter.isContextResolved()) {
                var contextResolver = dispatcher.config().getContextArgumentProviderFor(actualParameter);

                if (contextResolver != null) {
                    paramsInstances[i] = contextResolver.provide(context, actualParameter);
                    p--;
                    continue;
                }

                throw new IllegalStateException(
                        ("In class '%s', In method '%s', The parameter '%s' is set to be context resolved while not having a context resolver "
                                 + "for its type '%s'")
                                .formatted(method.getParent().getName(), method.getName(), actualParameter.getName(),
                                        actualParameter.getType().getTypeName())
                );
            } else {
                // not context resolved
                if (actualParameter.isAnnotationPresent(Flag.class) || actualParameter.isAnnotationPresent(Switch.class)) {
                    //flag parameters
                    Flag flag = actualParameter.getAnnotation(Flag.class);
                    Switch switchAnnotation = actualParameter.getAnnotation(Switch.class);

                    if (flag != null) {
                        paramsInstances[i] = context.getFlagValue(flag.value()[0]);
                    } else if (switchAnnotation != null) {
                        var switchParsedValue = context.getFlagValue(switchAnnotation.value()[0]);
                        if (switchParsedValue == null) {
                            throw new IllegalStateException(
                                    ("In class '%s', In method '%s', The parameter '%s' is annotated with @Switch but the switch '%s' is not "
                                             + "present in the command context")
                                            .formatted(method.getParent().getName(), method.getName(), actualParameter.getName(),
                                                    switchAnnotation.value()[0])
                            );
                        }
                        paramsInstances[i] = switchParsedValue;
                    }
                    p--;
                } else {
                    var argumentValue = context.getArgument(actualParameter.getName());
                    if (argumentValue != null) {
                        paramsInstances[i] = argumentValue;
                        continue;
                    }
                }
            }

        }
        return paramsInstances;
    }

    private static <S extends Source> @Nullable Argument<S> getUsageParam(List<? extends Argument<S>> params, int index) {
        if (index < 0 || index >= params.size()) {
            return null;
        }
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

        if (named != null) {
            name = named.value();
        } else if (flag != null) {
            name = flag.value()[0];
        } else if (switchAnnotation != null) {
            name = switchAnnotation.value()[0];
        } else {
            name = parameter.getElement().getName();
        }

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

    public static @NotNull DefaultValueProvider getOptionalValueSupplier(
            Class<? extends DefaultValueProvider> supplierClass
    ) throws NoSuchMethodException, InstantiationException,
                     IllegalAccessException, InvocationTargetException {

        var emptyConstructor = supplierClass.getDeclaredConstructor();
        emptyConstructor.setAccessible(true);

        return emptyConstructor.newInstance();
    }

    public static @NotNull DefaultValueProvider deduceOptionalValueSupplier(
            ParameterElement parameter,
            Default defaultAnnotation,
            DefaultProvider provider,
            DefaultValueProvider fallback
    ) throws CommandException {

        if (defaultAnnotation != null) {
            String def = defaultAnnotation.value();
            return DefaultValueProvider.of(def);
        } else if (provider != null) {
            Class<? extends DefaultValueProvider> supplierClass = provider.value();
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

    /*public static boolean isHelpParameter(ParameterElement element) {
        return TypeUtility.areRelatedTypes(element.getType(), CommandHelp.class);
    }*/

    /**
     * Checks if the type is not a concrete class (Interface, enum, abstract class, etc.)
     * @param parseElement the parse element
     * @return true if the type is not a concrete class, false otherwise
     */
    public static boolean isAbnormalClass(ParseElement<?> parseElement) {
        if (parseElement instanceof ClassElement classElement) {
            return isAbnormalClass(classElement.getElement());
        }
        return false;
    }

    public static boolean isAbnormalClass(Class<?> element) {
        return element.isInterface() || element.isEnum() || Modifier.isAbstract(element.getModifiers());
    }
}
