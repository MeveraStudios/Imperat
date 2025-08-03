package studio.mevera.imperat.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;

import static java.lang.reflect.Modifier.isStatic;

public final class Preconditions {

    private Preconditions() {
    }

    public static <T> void notEmpty(T[] array, String err) {
        if (array.length == 0)
            throw new IllegalStateException(err);
    }

    public static <T> void notEmpty(Collection<T> collection, String err) {
        if (collection.isEmpty())
            throw new IllegalStateException(err);
    }

    public static void notEmpty(String s, String err) {
        if (s.isEmpty())
            throw new IllegalStateException(err);
    }

    public static void checkArgument(boolean expr, String err) {
        if (!expr)
            throw new IllegalArgumentException(err);
    }

    public static int betweenIn(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static int getLeast(int value, int max) {
        return Math.min(value, max);
    }

    public static int getHighest(int value, int min) {
        return Math.max(value, min);
    }

    public static <T> void notNull(T t, String err) {
        if (t == null) {
            throw new NullPointerException(err + " cannot be null !");
        }
    }

    public static void checkCallableStatic(@Nullable Object instance, @NotNull Method method) {
        if (instance == null && !isStatic(method.getModifiers()))
            throw new IllegalArgumentException("The given method is not static, and no instance was provided. "
                + "Either mark the function as static with @JvmStatic, or pass the object/companion object value for the instance.");
    }
}