package studio.mevera.imperat.util;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.context.Source;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TypeWrap<T> {

    private final Type type;
    private final Class<?> rawType;

    protected TypeWrap() {
        this.type = extractType();
        this.rawType = extractRawType(type);
    }

    private TypeWrap(final Type type) {
        this.type = type;
        this.rawType = extractRawType(type);
    }

    public static TypeWrap<?> of(final Type type) {
        return new TypeWrap<>(type) {
        };
    }

    public static <T> TypeWrap<T> of(final Class<T> type) {
        return new TypeWrap<>(type) {
        };
    }

    private static Bounds any(Type[] bounds) {
        return new Bounds(bounds, true);
    }
    
    public static <S extends Source> TypeWrap<?> ofParameterized(Type rawClass, List<ParameterType<S,?>> genericParamTypes) {
        if(!(rawClass instanceof Class<?> clazz)) {
            throw new IllegalArgumentException("Raw class must be a class.");
        }

        if(genericParamTypes.isEmpty()) {
            return of(rawClass);
        }

        Type[] typeArgs = new Type[genericParamTypes.size()];
        for(int i = 0; i < genericParamTypes.size(); i++) {
            typeArgs[i] = genericParamTypes.get(i).type();
        }

        ParameterizedType parameterizedType = new ParameterizedType() {
            @Override
            public Type @NotNull [] getActualTypeArguments() {
                return typeArgs;
            }

            @Override
            public Type getRawType() {
                return clazz;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        return of(parameterizedType);
    }
    
    public static TypeWrap<?> ofArray(Type type) {
        Type arrayType = Array.newInstance(of(type).getRawType(), 0).getClass();
        return of(arrayType);
    }
    
    private Type extractType() {
        final Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getActualTypeArguments()[0];
        } else if (superclass instanceof Class) {
            return Object.class;
        } else {
            throw new IllegalArgumentException("TypeWrap must be created with a parameterized valueType.");
        }
    }

    private Class<?> extractRawType(Type type) {
        if (type == null)
            return null;

        if (type instanceof Class<?> cls)
            return cls;

        if (type instanceof ParameterizedType parameterizedType)
            return (Class<?>) parameterizedType.getRawType();

        if (type instanceof GenericArrayType genericArrayType)
            return Array.newInstance(extractRawType(genericArrayType.getGenericComponentType()), 0).getClass();

        return null;
    }

    public Type[] getParameterizedTypes() {
        if(type == null) {
            return null;
        }

        if(type instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getActualTypeArguments();
        }else {
            return null;
        }
    }

    public Type getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public Class<? super T> getRawType() {
        return (Class<? super T>) rawType;
    }

    @SuppressWarnings({"unchecked"})
    private Set<Class<? super T>> getRawTypes() {
        final Set<Class<? super T>> builder = new HashSet<>();

        new TypeVisitor() {
            @Override
            void visitTypeVariable(TypeVariable<?> t) {
                visit(t.getBounds());
            }

            @Override
            void visitWildcardType(WildcardType t) {
                visit(t.getUpperBounds());
            }

            @Override
            void visitParameterizedType(ParameterizedType t) {
                builder.add((Class<? super T>) t.getRawType());
            }

            @Override
            void visitClass(Class<?> t) {
                builder.add((Class<? super T>) t);
            }

            @Override
            void visitGenericArrayType(GenericArrayType t) {
                builder.add((Class<? super T>) TypeUtility.getArrayClass(of(t.getGenericComponentType()).getRawType()));
            }
        }.visit(type);
        return builder;
    }

    public final boolean isPrimitive() {
        return TypeUtility.isPrimitive(type);
    }

    public final boolean isWrapped() {
        return TypeUtility.isBoxed(type);
    }

    public TypeWrap<?> wrap() {
        if (this.isPrimitive()) {
            return of(TypeUtility.primitiveToBoxed(type));
        }
        return this;
    }

    public TypeWrap<?> unwrap() {
        if (this.isWrapped()) {
            return of(TypeUtility.boxedToPrimative(type));
        }
        return this;
    }

    public final boolean isArray() {
        return getComponentType() != null;
    }

    public TypeWrap<?> getComponentType() {
        Type componentType = TypeUtility.getComponentType(type);
        if (componentType == null) {
            return null;
        }
        return of(componentType);
    }

    public final boolean isSupertypeOf(TypeWrap<?> type) {
        return type.isSubtypeOf(getType());
    }

    public final boolean isSupertypeOf(Type type) {
        return of(type).isSubtypeOf(getType());
    }

    public final boolean isSubtypeOf(TypeWrap<?> type) {
        return isSubtypeOf(type.getType());
    }

    @SuppressWarnings("rawtypes")
    public final boolean isSubtypeOf(final Type supertype) {
        if (supertype == null) return false;

        if (supertype instanceof WildcardType) {
            return any(((WildcardType) supertype).getLowerBounds()).isSupertypeOf(type);
        }

        if (type instanceof WildcardType) {
            return any(((WildcardType) type).getUpperBounds()).isSubtypeOf(supertype);
        }

        if (type instanceof TypeVariable) {
            return type.equals(supertype) || any(((TypeVariable<?>) type).getBounds()).isSubtypeOf(supertype);
        }

        if (type instanceof GenericArrayType) {
            return of(supertype).isSupertypeOfArray((GenericArrayType) type);
        }

        if (supertype instanceof Class clazz) return this.someRawTypeIsSubclassOf(clazz);
        if (supertype instanceof ParameterizedType parameterizedType)
            return this.isSubtypeOfParameterizedType(parameterizedType); // TODO: Check if this checks actually work lol
        if (supertype instanceof GenericArrayType genericArrayType) return this.isSubtypeOfArrayType(genericArrayType);

        return false;
    }

    private boolean isSupertypeOfArray(GenericArrayType subtype) {
        if (type instanceof Class<?> thisClass) {
            if (!thisClass.isArray()) {
                return thisClass.isAssignableFrom(Object[].class);
            }
            return of(subtype.getGenericComponentType()).isSubtypeOf(thisClass.getComponentType());
        } else if (type instanceof GenericArrayType) {
            return of(subtype.getGenericComponentType())
                .isSubtypeOf(((GenericArrayType) type).getGenericComponentType());
        } else {
            return false;
        }
    }

    private boolean someRawTypeIsSubclassOf(Class<?> superclass) {
        for (Class<?> rawType : getRawTypes()) {
            if (superclass.isAssignableFrom(rawType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSubtypeOfParameterizedType(ParameterizedType supertype) {
        if (!(type instanceof ParameterizedType subType)) {
            return false;
        }

        Class<?> superRawType = (Class<?>) supertype.getRawType();
        Class<?> subRawType = (Class<?>) subType.getRawType();

        if (!superRawType.isAssignableFrom(subRawType)) {
            return false;
        }

        Type[] superTypeArgs = supertype.getActualTypeArguments();
        Type[] subTypeArgs = subType.getActualTypeArguments();

        for (int i = 0; i < superTypeArgs.length; i++) {
            if (!isTypeArgCompatible(subTypeArgs[i], superTypeArgs[i])) {
                return false;
            }
        }

        return isCompatibleOwnerType(supertype, subType);
    }

    private boolean isSubtypeOfArrayType(GenericArrayType supertype) {
        if (type instanceof Class<?> fromClass) {
            if (!fromClass.isArray()) {
                return false;
            }
            return of(fromClass.getComponentType()).isSubtypeOf(supertype.getGenericComponentType());
        } else if (type instanceof GenericArrayType fromArrayType) {
            return of(fromArrayType.getGenericComponentType())
                .isSubtypeOf(supertype.getGenericComponentType());
        } else {
            return false;
        }
    }

    private boolean isTypeArgCompatible(Type subArg, Type superArg) {
        if (superArg instanceof WildcardType wildcard) {
            return isWithinWildcardBounds(subArg, wildcard);
        } else if (superArg instanceof TypeVariable<?> var) {
            return isWithinTypeVarBounds(subArg, var);
        } else {
            return of(subArg).isSubtypeOf(superArg);
        }
    }

    private boolean isWithinWildcardBounds(Type type, WildcardType wildcard) {
        return isWithinBounds(type, wildcard.getLowerBounds(), wildcard.getUpperBounds());
    }

    private boolean isWithinTypeVarBounds(Type type, TypeVariable<?> typeVar) {
        return isWithinBounds(type, new Type[0], typeVar.getBounds());
    }

    private boolean isCompatibleOwnerType(ParameterizedType supertype, ParameterizedType subtype) {
        Type superOwner = supertype.getOwnerType();
        Type subOwner = subtype.getOwnerType();
        return superOwner == null ||
            (subOwner != null && of(subOwner).isSubtypeOf(superOwner));
    }

    private boolean isWithinBounds(Type type, Type[] lowerBounds, Type[] upperBounds) {
        for (Type lowerBound : lowerBounds) {
            if (!of(lowerBound).isSubtypeOf(type)) {
                return false;
            }
        }
        for (Type upperBound : upperBounds) {
            if (!of(type).isSubtypeOf(upperBound)) {
                return false;
            }
        }
        return true;
    }

    private record Bounds(Type[] bounds, boolean target) {

        boolean isSubtypeOf(Type supertype) {
            for (Type bound : bounds) {
                if (of(bound).isSubtypeOf(supertype) == target) {
                    return target;
                }
            }
            return !target;
        }

        boolean isSupertypeOf(Type subtype) {
            TypeWrap<?> type = of(subtype);
            for (Type bound : bounds) {
                if (type.isSubtypeOf(bound) == target) {
                    return target;
                }
            }
            return !target;
        }
    }

}
