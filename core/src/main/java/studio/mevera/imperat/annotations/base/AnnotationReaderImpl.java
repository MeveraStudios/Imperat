package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.ExternalSubCommand;
import studio.mevera.imperat.annotations.base.element.*;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ThrowableResolver;
import studio.mevera.imperat.util.ImperatDebugger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

@ApiStatus.Internal
final class AnnotationReaderImpl<S extends Source> implements AnnotationReader<S> {

    //private final Comparator<Method> METHOD_COMPARATOR;

    private final Imperat<S> imperat;
    private final AnnotationParser<S> parser;
    private final ElementSelector<MethodElement> methodSelector;
    private final RootCommandClass<S> rootCommandClass;
    private final ClassElement classElement;

    AnnotationReaderImpl(
        Imperat<S> imperat,
        ElementSelector<MethodElement> methodSelector,
        AnnotationParser<S> parser,
        Object instance
    ) {
        if(AnnotationHelper.isAbnormalClass(instance.getClass())) {
            throw new IllegalArgumentException("Failed to parse the abnormal class '%s'".formatted(instance.getClass().getTypeName()));
        }
        this.imperat = imperat;
        this.parser = parser;
        this.rootCommandClass = new RootCommandClass<>(instance.getClass(), instance);
        this.methodSelector = methodSelector;
        //METHOD_COMPARATOR = Comparator.comparingInt(m -> AnnotationHelper.loadMethodPriority(m, imperat.config()));
        this.classElement = read(imperat);
    }

    private @NotNull ClassElement read(Imperat<S> imperat) {
        return readClass(imperat, parser, null, rootCommandClass.proxyClass());
    }

    private @NotNull ClassElement readClass(
        Imperat<S> imperat,
        AnnotationParser<S> parser,
        @Nullable ClassElement parent,
        @NotNull Class<?> clazz
    ) {

        ClassElement root = parent == null ?
                new ClassElement(parser, null, clazz, rootCommandClass.proxyInstance())
                : new ClassElement(parser, parent, clazz);

        //Adding methods with their parameters
        List<Method> methods;
        try {
            methods = SourceOrderHelper.getMethodsInSourceOrder(clazz);
        } catch (Exception e) {
            ImperatDebugger.error(AnnotationReaderImpl.class, "readClass", e);
            throw new RuntimeException(e);
        }
        
        //Arrays.sort(methods, METHOD_COMPARATOR);
        for (Method method : methods) {
            MethodElement methodElement = new MethodElement(parser, root, method);
            if (methodSelector.canBeSelected(imperat, parser, methodElement, false)) {
                root.addChild(methodElement);
            }
        }

        //We add external subcommand classes from @ExternalSubCommand as children
        if (root.isAnnotationPresent(ExternalSubCommand.class)) {
            ExternalSubCommand externalSubCommand = root.getAnnotation(ExternalSubCommand.class);
            assert externalSubCommand != null;
            for (Class<?> subClass : externalSubCommand.value()) {
                root.addChild(
                    readClass(imperat, parser, root, subClass)
                );
            }
        }

        //Adding inner classes
        List<Class<?>> innerClasses;
        try {
            innerClasses = SourceOrderHelper.getInnerClassesInSourceOrder(clazz);

        } catch (Exception e) {
            ImperatDebugger.error(AnnotationReaderImpl.class, "readClass", e);
            throw new RuntimeException(e);
        }

        for (Class<?> child : innerClasses) {
            if(AnnotationHelper.isAbnormalClass(child)) {
                ImperatDebugger.debug("Ignoring abnormal sub class '%s'", child.getTypeName());
                continue;
            }
            root.addChild(
                    readClass(imperat, parser, root, child)
            );
        }

        return root;
    }


    @Override
    public RootCommandClass<S> getRootClass() {
        return rootCommandClass;
    }

    @Override
    public ClassElement getParsedClass() {
        return classElement;
    }

    @Override
    public void acceptCommandsParsing(CommandClassVisitor<S, Set<Command<S>>> visitor) {
        var collectedCommands = classElement.accept(visitor);
        if(collectedCommands == null) return;
        
        for (Command<S> loaded : collectedCommands) {
            imperat.registerCommand(loaded);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <E extends Throwable> void acceptThrowableResolversParsing(CommandClassVisitor<S, Set<MethodThrowableResolver<?, S>>> visitor) {
        Set<MethodThrowableResolver<?, S>> collectedErrorHandlers = classElement.accept(visitor);
        if(collectedErrorHandlers == null) {
            return;
        }
        for (var errorHandler : collectedErrorHandlers) {
            Class<E> castedExceptionType = (Class<E>) errorHandler.getExceptionType();
            imperat.config().setThrowableResolver(castedExceptionType, (MethodThrowableResolver<E, S>)errorHandler);
        }
    }
}
