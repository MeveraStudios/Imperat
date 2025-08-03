package studio.mevera.imperat.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

abstract class TypeVisitor {

    private final Set<Type> visited = new HashSet<>();

    public final void visit(@Nullable Type... types) {
        for (final Type type : types) {
            if (type == null || !visited.add(type)) {
                // null owner valueType, or already visited;
                continue;
            }

            boolean succeeded = false;
            try {
                if (type instanceof TypeVariable) {
                    visitTypeVariable((TypeVariable<?>) type);
                } else if (type instanceof WildcardType) {
                    visitWildcardType((WildcardType) type);
                } else if (type instanceof ParameterizedType) {
                    visitParameterizedType((ParameterizedType) type);
                } else if (type instanceof Class) {
                    visitClass((Class<?>) type);
                } else if (type instanceof GenericArrayType) {
                    visitGenericArrayType((GenericArrayType) type);
                } else {
                    throw new AssertionError("Unknown valueType: " + type);
                }
                succeeded = true;
            } finally {
                if (!succeeded) {
                    visited.remove(type);
                }
            }
        }
    }

    void visitClass(Class<?> t) {
    }

    void visitGenericArrayType(GenericArrayType t) {
    }

    void visitParameterizedType(ParameterizedType t) {
    }

    void visitTypeVariable(TypeVariable<?> t) {
    }

    void visitWildcardType(WildcardType t) {
    }

}
