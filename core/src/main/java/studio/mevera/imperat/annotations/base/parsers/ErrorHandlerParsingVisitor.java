package studio.mevera.imperat.annotations.base.parsers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.annotations.types.ExceptionHandler;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.asm.DefaultMethodCallerFactory;
import studio.mevera.imperat.util.asm.MethodCaller;

import java.util.HashSet;
import java.util.Set;

final class ErrorHandlerParsingVisitor<S extends Source> extends CommandClassParser<S, Set<MethodCommandExceptionHandler<?, S>>> {

    ErrorHandlerParsingVisitor(
            Imperat<S> imperat,
            AnnotationParser<S> parser,
            ElementSelector<MethodElement> methodSelector
    ) {
        super(imperat, parser, methodSelector);
    }

    @SuppressWarnings("unchecked")
    public static <S extends Source, E extends Throwable> @Nullable MethodCommandExceptionHandler<E, S> loadErrorHandler(
            ImperatConfig<S> cfg,
            ClassElement owner,
            MethodElement methodElement
    ) {
        try {
            var ann = methodElement.getAnnotation(ExceptionHandler.class);
            if (ann == null) {
                return null;
            }

            Class<E> exceptionType = (Class<E>) ann.value();
            MethodCaller.BoundMethodCaller caller = DefaultMethodCallerFactory.INSTANCE.createFor(methodElement.getElement())
                                                            .bindTo(owner.getObjectInstance());

            return new MethodCommandExceptionHandler<>(caller, exceptionType);
        } catch (Throwable e) {
            ImperatDebugger.warning("Failed to register throwable-method '" + methodElement.getName() + "' in class '" + owner.getChildren() + "'");
            cfg.getThrowablePrinter().print(e);
            return null;
        }
    }

    @Override
    public Set<MethodCommandExceptionHandler<?, S>> visitCommandClass(@NotNull ClassElement clazz) {
        Set<MethodCommandExceptionHandler<?, S>> throwableResolvers = new HashSet<>();
        for (var childElement : clazz.getChildren()) {
            if (!(childElement instanceof MethodElement methodElement)) {
                continue;
            }
            if (methodSelector.canBeSelected(imperat, parser, methodElement, false)) {
                var resolverLoaded = loadErrorHandler(imperat.config(), clazz, methodElement);
                if (resolverLoaded != null) {
                    throwableResolvers.add(resolverLoaded);
                }
            }
        }
        return throwableResolvers;
    }
}
