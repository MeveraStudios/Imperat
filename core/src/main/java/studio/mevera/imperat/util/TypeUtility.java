package studio.mevera.imperat.util;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class TypeUtility {

    static Set<Class<?>> ACCEPTED_GENERIC_WRAPPERS = Set.of(
            CompletableFuture.class,
            Optional.class
    );

    static Set<Type> NUMERIC_PRIMITIVES = Set.of(
            short.class, byte.class, int.class,
            long.class, float.class, double.class
    );

    static Map<Type, Type> PRIMITIVES_TO_BOXED = Map.of(
            boolean.class, Boolean.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            byte.class, Byte.class
    );

    static Map<Type, Type> BOXED_TO_PRIMITIVES = Map.of(
            Boolean.class, boolean.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class,
            Byte.class, byte.class
    );

    private TypeUtility() {
    }

    public static boolean isInteger(String string) {
        if (string == null) {
            return false;
        }
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isBoolean(String string) {
        if (string == null) {
            return false;
        }
        return Boolean.parseBoolean(string);
    }

    public static boolean isFloat(String input) {
        try {
            Float.parseFloat(input);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isDouble(String str) {
        if (str == null) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isShort(String str) {
        if (str == null) {
            return false;
        }
        try {
            Short.parseShort(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isByte(String str) {
        if (str == null) {
            return false;
        }
        try {
            Byte.parseByte(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isLong(String str) {
        if (str == null) {
            return false;
        }
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNumericType(Class<?> type) {
        return Number.class.isAssignableFrom(type) || (NUMERIC_PRIMITIVES.contains(type));
    }

    public static boolean isNumericType(TypeWrap<?> token) {
        return NUMERIC_PRIMITIVES.contains(token.unwrap().getType());
    }

    public static boolean isPrimitive(final Type type) {
        return PRIMITIVES_TO_BOXED.get(type) != null;
    }

    public static boolean isBoxed(final Type type) {
        return BOXED_TO_PRIMITIVES.get(type) != null;
    }

    public static @NotNull Type primitiveToBoxed(Type primitive) {
        return PRIMITIVES_TO_BOXED.getOrDefault(primitive, primitive);
    }

    public static @NotNull Type boxedToPrimative(Type boxed) {
        return BOXED_TO_PRIMITIVES.getOrDefault(boxed, boxed);
    }

    public static boolean matches(@NotNull Type type1, @NotNull Type type2) {
        var t1 = isPrimitive(type1) ? primitiveToBoxed(type1) : type1;
        var t2 = isPrimitive(type2) ? primitiveToBoxed(type2) : type2;
        return t1.equals(t2);
    }

    public static Type getInsideGeneric(Type genericType, Type fallback) {
        try {
            return ((ParameterizedType) genericType).getActualTypeArguments()[0];
        } catch (ClassCastException e) {
            return fallback;
        }
    }

    public static Type getComponentType(final Type type) {
        final AtomicReference<Type> result = new AtomicReference<>();
        new TypeVisitor() {
            @Override
            protected void visitGenericArrayType(GenericArrayType t) {
                result.set(t.getGenericComponentType());
            }

            @Override
            protected void visitClass(Class<?> t) {
                result.set(t.getComponentType());
            }

            @Override
            protected void visitTypeVariable(TypeVariable<?> t) {
                result.set(subtypeOfComponentType(t.getBounds()));
            }

            @Override
            protected void visitWildcardType(WildcardType t) {
                result.set(subtypeOfComponentType(t.getUpperBounds()));
            }

            private Type subtypeOfComponentType(Type[] bounds) {
                // Assuming you want the first bound for simplicity
                return bounds.length > 0 ? bounds[0] : null;
            }
        }.visit(type);
        return result.get();
    }

    static Class<?> getArrayClass(Class<?> componentType) {
        // TODO(user): This is not the most efficient way to handle generic
        // arrays, but is there another way to extract the array class in a
        // non-hacky way (i.e. using String value class names- "[L...")?
        return Array.newInstance(componentType, 0).getClass();
    }

    public static boolean areRelatedTypes(Type type1, Type type2) {
        return matches(type1, type2)
                       || TypeWrap.of(type1).isSupertypeOf(type2)
                       || TypeWrap.of(type2).isSupertypeOf(type1);
    }

    public static boolean isNumber(String str) {
        return isDouble(str);
    }

    public static boolean isAcceptableGreedyWrapper(Type type) {
        Preconditions.notNull(type, "type");
        return ACCEPTED_GENERIC_WRAPPERS.contains(TypeWrap.of(type).getRawType());
    }

    public static boolean hasGenericType(@NotNull Type type, @NotNull Type genericToFind) {
        TypeWrap<?> typeWrap = TypeWrap.of(type);
        Type[] typeParams = typeWrap.getParameterizedTypes();
        if (typeParams == null) {
            return false;
        }

        for (Type genericTypeParam : typeParams) {
            if (genericTypeParam.equals(genericToFind)) {
                return true;
            }
        }
        return false;
    }

    public static <S extends Source> boolean areTrueFlagsOfSameInputType(Set<FlagParameter<S>> flagDataSet) {
        ParameterType<S, ?> inputType = null;
        for (FlagParameter<S> flagParam : flagDataSet) {
            FlagData<S> flagData = flagParam.flagData();
            ParameterType<S, ?> otherInputType = flagData.inputType();
            if (otherInputType == null) {
                return false;
            }

            if (inputType != null && !otherInputType.equalsExactly(inputType.type())) {
                return false;
            }
            inputType = flagData.inputType();
        }

        return true;
    }
}
