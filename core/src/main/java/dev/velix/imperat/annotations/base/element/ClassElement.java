package dev.velix.imperat.annotations.base.element;

import dev.velix.imperat.annotations.Dependency;
import dev.velix.imperat.annotations.base.AnnotationParser;
import dev.velix.imperat.command.Command;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.util.reflection.Reflections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class ClassElement extends ParseElement<Class<?>> {

    private final Set<ParseElement<?>> children = new LinkedHashSet<>();
    private final Object instance;

    public <S extends Source> ClassElement(
        @NotNull AnnotationParser<S> parser,
        @NotNull ClassElement parent,
        @NotNull Class<?> element
    ) {
        super(parser, parent, element);
        this.instance = newInstance(parent);
        this.injectDependencies();
    }

    public <S extends Source> ClassElement(
            @NotNull AnnotationParser<S> parser,
            @Nullable ClassElement parent,
            @NotNull Class<?> element,
            @NotNull Object instance
    ) {
        super(parser, parent, element);
        this.instance = instance;
        this.injectDependencies();
    }

    private void injectDependencies() {
        Exception exception = null;
        for (Field field : element.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Dependency.class)) {
                continue;
            }

            if (Modifier.isFinal(field.getModifiers())) {
                throw new IllegalArgumentException("Field '" + field.getName() + "' cannot be declared final while being annotated with `@Dependency`");
            }

            field.setAccessible(true);
            try {
                var supplied = parser.getImperat().config().resolveDependency(field.getType());
                field.set(instance, supplied);
            } catch (IllegalAccessException e) {
                exception = e;
                break;
            }
        }

        if (exception != null) {
            throw new RuntimeException(exception);
        }
    }
    
    private Object newInstance(ClassElement parent, Object... constructorArgs) {
        boolean isStaticClass = this.isStaticClass();
        boolean external = !this.element.isMemberClass();
        
        try {
            Constructor<?> cons;
            Object[] finalArgs;
            
            if (isStaticClass || external) {
                // Static inner class - doesn't need outer instance
                Class<?>[] types = new Class[constructorArgs.length];
                for (int i = 0; i < types.length; i++) {
                    types[i] = constructorArgs[i].getClass();
                }
                
                cons = Reflections.getConstructor(element, types);
                finalArgs = constructorArgs;
                
            } else {
                // Non-static inner class - needs outer instance as first parameter
                if (parent == null) {
                    throw new IllegalArgumentException("Non-static inner class " + element.getSimpleName() +
                            " requires a parent instance, but parent is null");
                }
                
                Object parentInstance = parent.getObjectInstance();
                if (parentInstance == null) {
                    throw new IllegalArgumentException("Parent instance is null for non-static inner class " +
                            element.getSimpleName());
                }
                
                // For non-static inner classes, first parameter is always the outer class instance
                Class<?>[] types = new Class[constructorArgs.length + 1];
                types[0] = parent.getElement(); // Outer class type
                
                for (int i = 0; i < constructorArgs.length; i++) {
                    types[i + 1] = constructorArgs[i].getClass();
                }
                
                cons = Reflections.getConstructor(element, types);
                
                // Build final arguments array with parent instance first
                finalArgs = new Object[constructorArgs.length + 1];
                finalArgs[0] = parentInstance;
                System.arraycopy(constructorArgs, 0, finalArgs, 1, constructorArgs.length);
            }
            
            if (cons == null) {
                throw new IllegalCallerException("Class " + element.getSimpleName() +
                        " doesn't have a constructor matching the arguments");
            }
            
            return cons.newInstance(finalArgs);
            
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create instance of " + element.getSimpleName(), e);
        }
    }
    
    public boolean isStaticClass() {
        return Modifier.isStatic(this.element.getModifiers());
    }
    
    public Object getObjectInstance() {
        return instance;
    }

    public <S extends Source> Set<Command<S>> accept(CommandClassVisitor<S> visitor) {
        try {
            return visitor.visitCommandClass(this);
        } catch (Throwable ex) {
            ex.printStackTrace();
            return Collections.emptySet();
        }
    }

    public @Nullable ParseElement<?> getChildElement(Predicate<ParseElement<?>> predicate) {
        for (var element : getChildren()) if (predicate.test(element)) return element;
        return null;
    }

    public void addChild(ParseElement<?> element) {
        children.add(element);
    }

    public boolean isRootClass() {
        return getParent() == null;
    }

    @Override
    public String getName() {
        return getElement().getName();
    }

    public String getSimpleName() {
        return getElement().getSimpleName();
    }

    public Set<ParseElement<?>> getChildren() {
        return children;
    }

}
