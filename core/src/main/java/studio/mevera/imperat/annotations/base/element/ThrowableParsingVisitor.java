package studio.mevera.imperat.annotations.base.element;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.ExceptionHandler;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.asm.DefaultMethodCallerFactory;
import studio.mevera.imperat.util.asm.MethodCaller;
import java.util.HashSet;
import java.util.Set;

final class ThrowableParsingVisitor<S extends Source> extends CommandClassVisitor<S, Set<MethodThrowableResolver<?, S>>>{
    
    ThrowableParsingVisitor(
            Imperat<S> imperat,
            AnnotationParser<S> parser,
            ElementSelector<MethodElement> methodSelector
    ) {
        super(imperat, parser, methodSelector);
    }
    
    @Override
    public Set<MethodThrowableResolver<?, S>> visitCommandClass(@NotNull ClassElement clazz) {
        Set<MethodThrowableResolver<?, S>> throwableResolvers = new HashSet<>();
        System.out.println("CLASS '" + clazz.getName() +"' has " + clazz.getChildren().size() + " children");
        for(var childElement : clazz.getChildren()) {
            System.out.println("Class '" + clazz.getName() + "' has child '" + childElement.getName() + "' of type : " + childElement.element.getClass().getTypeName());
            if(! (childElement instanceof MethodElement methodElement))
                continue;
            System.out.println("Checking method '" + methodElement.getName() +"'");
            if(methodSelector.canBeSelected(imperat, parser, methodElement, false)) {
                var resolverLoaded = loadResolver(clazz, methodElement);
                if(resolverLoaded != null) {
                    System.out.println("Adding loaded throwable resolver for exception: '" + resolverLoaded.getExceptionType().getTypeName() + "'");
                    throwableResolvers.add(resolverLoaded);
                }
            }
        }
        return throwableResolvers;
    }
    
    @SuppressWarnings("unchecked")
    private <E extends Throwable> @Nullable MethodThrowableResolver<E, S> loadResolver(ClassElement owner, MethodElement methodElement) {
        try {
            var ann = methodElement.getAnnotation(ExceptionHandler.class);
            if(ann == null)return null;
            
            Class<E> exceptionType = (Class<E>) ann.value();
            MethodCaller.BoundMethodCaller caller = DefaultMethodCallerFactory.INSTANCE.createFor(methodElement.getElement())
                    .bindTo(owner.getObjectInstance());
            
            return new MethodThrowableResolver<>(caller, exceptionType);
        } catch (Throwable e) {
            ImperatDebugger.warning("Failed to register throwable-method '" + methodElement.getName() + "' in class '" + owner.getChildren() + "'");
            e.printStackTrace();
            return null;
        }
    }
}
