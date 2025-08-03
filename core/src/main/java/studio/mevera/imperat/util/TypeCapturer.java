package studio.mevera.imperat.util;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

public abstract class TypeCapturer {

    /**
     * Extract generic parameter type from this class's direct superclass.
     *
     * @param index index of the generic parameter
     * @return resolved Type
     */
    protected Type extractType(int index) {
        Type genericSuperClass = getClass().getGenericSuperclass();

        if (genericSuperClass instanceof ParameterizedType parameterized) {
            Type[] args = parameterized.getActualTypeArguments();
            if (index < 0 || index >= args.length)
                throw new IndexOutOfBoundsException("No type argument at index " + index);

            return resolveType(args[index]);
        }

        throw new IllegalStateException("Superclass is not parameterized: " + genericSuperClass);
    }

    /**
     * Extract generic parameter type from a specific superclass/interface in the hierarchy.
     *
     * @param targetSuperclass superclass to extract from
     * @param index           index of generic parameter
     * @return resolved Type
     */
    protected Type extractType(Class<?> targetSuperclass, int index) {
        Class<?> current = getClass();

        while (current != null && current != Object.class) {
            Type genericSuper = current.getGenericSuperclass();

            if (genericSuper instanceof ParameterizedType parameterized) {
                Class<?> raw = (Class<?>) parameterized.getRawType();

                if (raw.equals(targetSuperclass)) {
                    Type[] args = parameterized.getActualTypeArguments();
                    if (index < 0 || index >= args.length)
                        throw new IndexOutOfBoundsException("No type argument at index " + index);

                    return resolveType(args[index]);
                }

                current = raw;
            } else if (genericSuper instanceof Class<?> raw) {
                current = raw;
            } else {
                break;
            }
        }

        throw new IllegalStateException(
                "Superclass " + targetSuperclass.getName() + " not found in hierarchy of " + getClass().getName());
    }

    /**
     * Resolves a type by recursively substituting TypeVariables from the class hierarchy starting at getClass().
     */
    private Type resolveType(Type type) {
        if (type instanceof Class<?>) {
            return type;
        }

        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            Type[] resolvedArgs = new Type[args.length];
            for (int i = 0; i < args.length; i++) {
                resolvedArgs[i] = resolveType(args[i]);
            }
            return new ResolvedParameterizedType((Class<?>) pt.getRawType(), resolvedArgs, pt.getOwnerType());
        }

        if (type instanceof GenericArrayType gat) {
            Type comp = resolveType(gat.getGenericComponentType());
            if (comp instanceof Class<?> cls) {
                return Array.newInstance(cls, 0).getClass();
            }
            return new ResolvedGenericArrayType(comp);
        }

        if (type instanceof TypeVariable<?> tv) {
            return resolveTypeVariable(tv, getClass(), new HashMap<>());
        }

        return type;
    }

    /**
     * Resolves a TypeVariable to a concrete Type by building a substitution map walking up the class hierarchy.
     * @param target TypeVariable to resolve
     * @param startClass class to start resolution from (always getClass())
     * @param typeMap map of TypeVariable -> Type substitutions so far
     * @return resolved Type or original TypeVariable if unresolved
     */
    private Type resolveTypeVariable(TypeVariable<?> target, Class<?> startClass, Map<TypeVariable<?>, Type> typeMap) {
        Class<?> current = startClass;

        while (current != null && current != Object.class) {
            Type superclass = current.getGenericSuperclass();
            if (!(superclass instanceof ParameterizedType parameterized)) {
                current = current.getSuperclass();
                continue;
            }

            Class<?> raw = (Class<?>) parameterized.getRawType();
            Type[] actualArgs = parameterized.getActualTypeArguments();
            TypeVariable<?>[] declaredVars = raw.getTypeParameters();

            for (int i = 0; i < declaredVars.length; i++) {
                Type actual = actualArgs[i];

                if (actual instanceof TypeVariable<?> tv && typeMap.containsKey(tv)) {
                    typeMap.put(declaredVars[i], typeMap.get(tv));
                } else {
                    typeMap.put(declaredVars[i], actual);
                }
            }

            current = raw;
        }

        Type resolved = typeMap.get(target);

        if (resolved instanceof TypeVariable<?> nested) {
            return resolveTypeVariable(nested, startClass, typeMap);
        }

        return resolved != null ? resolved : target;
    }

    private Type substituteTypeVariables(Type type, Map<TypeVariable<?>, Type> typeVarAssigns) {
        if (type instanceof TypeVariable<?> tv) {
            Type mapped = typeVarAssigns.get(tv);
            if (mapped != null && !mapped.equals(tv)) {
                return substituteTypeVariables(mapped, typeVarAssigns);
            }
            return tv;
        }

        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            Type[] resolved = new Type[args.length];
            for (int i = 0; i < args.length; i++) {
                resolved[i] = substituteTypeVariables(args[i], typeVarAssigns);
            }
            return new ResolvedParameterizedType((Class<?>) pt.getRawType(), resolved, pt.getOwnerType());
        }

        if (type instanceof GenericArrayType gat) {
            Type comp = substituteTypeVariables(gat.getGenericComponentType(), typeVarAssigns);
            if (comp instanceof Class<?> cls) {
                return Array.newInstance(cls, 0).getClass();
            }
            return new ResolvedGenericArrayType(comp);
        }

        return type;
    }

    private static class ResolvedParameterizedType implements ParameterizedType {
        private final Class<?> raw;
        private final Type[] args;
        private final Type owner;

        ResolvedParameterizedType(Class<?> raw, Type[] args, Type owner) {
            this.raw = raw;
            this.args = args;
            this.owner = owner;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return args;
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return owner;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(raw.getTypeName());
            if (args.length > 0) {
                sb.append("<");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(args[i].getTypeName());
                }
                sb.append(">");
            }
            return sb.toString();
        }
    }

    private static class ResolvedGenericArrayType implements GenericArrayType {
        private final Type component;

        ResolvedGenericArrayType(Type component) {
            this.component = component;
        }

        @Override
        public Type getGenericComponentType() {
            return component;
        }

        @Override
        public String toString() {
            return component.getTypeName() + "[]";
        }
    }
}
