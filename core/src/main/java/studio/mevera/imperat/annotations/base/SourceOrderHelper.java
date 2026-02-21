package studio.mevera.imperat.annotations.base;

import studio.mevera.imperat.annotations.ParseOrder;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SourceOrderHelper {

    private SourceOrderHelper() {
        throw new AssertionError();
    }

    private static int priorityOf(AnnotatedElement element) {
        ParseOrder parseOrder = element.getAnnotation(ParseOrder.class);
        return parseOrder != null ? parseOrder.value() : Integer.MAX_VALUE;
    }

    /**
     * Gets methods in order based on @Priority value (lowest = first),
     * and includes all methods if no annotation is present.
     */
    @SuppressWarnings("all")
    public static List<Method> getMethodsInSourceOrder(Class<?> clazz) {
        Method[] declared = clazz.getDeclaredMethods();
        Map<Method, Integer> declarationOrder = new LinkedHashMap<>(declared.length);
        List<Method> methods = new ArrayList<>(declared.length);

        for (Method method : declared) {
            if (method.isSynthetic()) {
                continue;
            }

            declarationOrder.put(method, declarationOrder.size());
            methods.add(method);
        }

        methods.sort(
                Comparator.<Method>comparingInt(SourceOrderHelper::priorityOf)
                        .thenComparingInt(method -> declarationOrder.getOrDefault(method, Integer.MAX_VALUE))
                        .thenComparing(Method::getName)
                        .thenComparingInt(Method::getParameterCount)
        );
        return methods;
    }

    /**
     * Gets inner classes (both static and non-static) based on @Priority annotations.
     * Falls back to reflection order if no annotation present.
     */
    @SuppressWarnings("all")
    public static List<Class<?>> getInnerClassesInSourceOrder(Class<?> outerClass) {
        Map<Class<?>, Integer> declarationOrder = new LinkedHashMap<>();

        for (Class<?> declared : outerClass.getDeclaredClasses()) {
            if (declared.isSynthetic()) {
                continue;
            }
            declarationOrder.putIfAbsent(declared, declarationOrder.size());
        }

        for (Class<?> nest : outerClass.getNestMembers()) {
            if (nest.equals(outerClass)) {
                continue;
            }
            if (nest.getEnclosingClass() != outerClass || nest.isSynthetic()) {
                continue;
            }
            declarationOrder.putIfAbsent(nest, declarationOrder.size());
        }

        List<Class<?>> innerClasses = new ArrayList<>(declarationOrder.keySet());
        innerClasses.sort(
                Comparator.<Class<?>>comparingInt(SourceOrderHelper::priorityOf)
                        .thenComparingInt(clazz -> declarationOrder.getOrDefault(clazz, Integer.MAX_VALUE))
                        .thenComparing(Class::getName)
        );
        return innerClasses;
    }

}
