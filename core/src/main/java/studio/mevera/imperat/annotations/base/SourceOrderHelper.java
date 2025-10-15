package studio.mevera.imperat.annotations.base;

import studio.mevera.imperat.annotations.Priority;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public final class SourceOrderHelper {

    private static final Comparator<AnnotatedElement> PRIORITY_COMPARATOR = Comparator
            .comparingInt((AnnotatedElement e) -> {
                Priority p = e.getAnnotation(Priority.class);
                return p != null ? p.value() : Integer.MAX_VALUE;
            })
            .thenComparing(e -> {
                if (e instanceof Class<?>) {
                    return ((Class<?>) e).getSimpleName();
                } else if (e instanceof Method) {
                    return ((Method) e).getName();
                } else {
                    return "";
                }
            });

    private SourceOrderHelper() {
        throw new AssertionError();
    }

    /**
     * Gets methods in order based on @Priority value (lowest = first),
     * and includes all methods if no annotation is present.
     */
    public static List<Method> getMethodsInSourceOrder(Class<?> clazz) {
        Method[] declared = clazz.getDeclaredMethods();
        List<Method> methods = new ArrayList<>(Arrays.asList(declared));
        methods.removeIf(Method::isSynthetic);
        methods.sort(PRIORITY_COMPARATOR);
        return methods;
    }

    /**
     * Gets inner classes (both static and non-static) based on @Priority annotations.
     * Falls back to reflection order if no annotation present.
     */
    public static List<Class<?>> getInnerClassesInSourceOrder(Class<?> outerClass) {
        Set<Class<?>> innerSet = new HashSet<>(Arrays.asList(outerClass.getDeclaredClasses()));

        for (Class<?> nest : outerClass.getNestMembers()) {
            if (!nest.equals(outerClass)) {
                innerSet.add(nest);
            }
        }

        List<Class<?>> innerClasses = new ArrayList<>(innerSet);
        innerClasses.sort(PRIORITY_COMPARATOR);
        return innerClasses;
    }

}
