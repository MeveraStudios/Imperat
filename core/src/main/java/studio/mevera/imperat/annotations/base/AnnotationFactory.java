package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.util.Preconditions;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Supplier;

/**
 * A class that is responsible for creating
 * dynamic instances of annotations of certain types
 * useful while implementing/using {@link AnnotationReplacer}
 */
@ApiStatus.AvailableSince("1.0.0")
public final class AnnotationFactory {

    /**
     * Creates a new annotation with no values. Any default values will
     * automatically be used.
     *
     * @param type The annotation valueType
     * @param <T>  Annotation valueType
     * @return The newly created annotation
     */
    public static @NotNull <T extends Annotation> T create(@NotNull Class<T> type) {
        return create(type, Collections.emptyMap());
    }

    /**
     * Creates a new annotation with the given map values. Any default values will
     * automatically be used if not specified in the map.
     * <p>
     * Note that the map may also use {@link Supplier}s instead of direct
     * values.
     *
     * @param type    The annotation valueType
     * @param members The annotation members
     * @param <T>     Annotation valueType
     * @return The newly created annotation
     */
    public static @NotNull <T extends Annotation> T create(@NotNull Class<T> type,
                                                           @NotNull Map<String, Object> members) {
        Preconditions.notNull(type, "type");
        Preconditions.notNull(members, "members");
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            new DynamicAnnotationHandler(type, members)
        ));
    }

    /**
     * Creates a new annotation with the given map values. Any default values will
     * automatically be used if not specified in the map.
     * <p>
     * Note that the map may also use {@link Supplier}s instead of direct
     * values.
     *
     * @param type The annotation valueType
     * @param <T>  Annotation valueType
     * @return The newly created annotation
     */
    public static @NotNull <T extends Annotation> T create(@NotNull Class<T> type,
                                                           @NotNull Object... members) {
        Preconditions.notNull(type, "type");
        Preconditions.notNull(members, "members");
        if (members.length % 2 != 0)
            throw new IllegalArgumentException("Cannot have a non-even amount of members! Found " + members.length);
        Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < members.length; i += 2) {
            String key = String.valueOf(members[i]);
            Object value = members[i + 1];
            values.put(key, value);
        }
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            new DynamicAnnotationHandler(type, values)
        ));
    }

    /**
     * Implementation of {@link Annotation#hashCode()}.
     *
     * @param type    The annotation valueType
     * @param members The annotation members
     * @return The annotation's hashcode.
     */
    private static int hashCode(Class<? extends Annotation> type, Map<String, Object> members) {
        int result = 0;
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName();
            Object value = members.get(name);
            result += (127 * name.hashCode()) ^ (Objects.hashCode(value) - 31);
        }
        return result;
    }

    /**
     * Implementation of {@link Annotation#equals(Object)}.
     *
     * @param type    The annotation valueType
     * @param members The annotation members
     * @param other   The other annotation to compare
     * @return if they are equal
     */
    private static boolean equals(Class<? extends Annotation> type, Map<String, Object> members, Object other) throws Exception {
        if (!type.isInstance(other)) {
            return false;
        }
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName();
            if (!Objects.deepEquals(method.invoke(other), members.get(name))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Implementation of {@link Annotation#toString()}.
     *
     * @param type    The annotation valueType
     * @param members The annotation members
     * @return The annotation's hashcode.
     */
    private static String toString(Class<? extends Annotation> type, Map<String, Object> members) {
        StringBuilder sb = new StringBuilder().append('@').append(type.getName()).append('(');
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, Object> entry : members.entrySet()) {
            joiner.add(entry.getKey() + "=" + deepToString(entry.getValue()));
        }
        sb.append(joiner);
        return sb.append(')').toString();
    }

    private static String deepToString(Object arg) {
        String s = Arrays.deepToString(new Object[]{arg});
        return s.substring(1, s.length() - 1); // cut off the []
    }

    private record DynamicAnnotationHandler(Class<? extends Annotation> annotationType,
                                            Map<String, Object> annotationMembers) implements InvocationHandler {

        private DynamicAnnotationHandler(Class<? extends Annotation> annotationType, Map<String, Object> annotationMembers) {
            this.annotationType = annotationType;
            this.annotationMembers = new HashMap<>(annotationMembers);
            for (Method method : annotationType.getDeclaredMethods()) {
                this.annotationMembers.putIfAbsent(method.getName(), method.getDefaultValue());
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "toString":
                    return AnnotationFactory.toString(annotationType, annotationMembers);
                case "hashCode":
                    return AnnotationFactory.hashCode(annotationType, annotationMembers);
                case "equals":
                    return AnnotationFactory.equals(annotationType, annotationMembers, args[0]);
                case "annotationType":
                    return annotationType;
                default: {
                    Object v = annotationMembers.get(method.getName());
                    if (v == null)
                        throw new AbstractMethodError(method.getName());
                    return v instanceof Supplier ? ((Supplier<?>) v).get() : v;
                }
            }
        }
    }

}
