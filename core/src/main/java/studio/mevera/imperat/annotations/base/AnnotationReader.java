package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.CommandClassVisitor;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.MethodThrowableResolver;
import studio.mevera.imperat.annotations.base.element.RootCommandClass;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;

import java.util.Set;

/**
 * Represents a class that has a single responsibility of
 * reading the annotation components of an annotated command class
 *
 * @author Mqzen
 */
@ApiStatus.AvailableSince("1.0.0")
public interface AnnotationReader<S extends Source> {

    static <S extends Source> AnnotationReader<S> read(
            Imperat<S> imperat,
            ElementSelector<MethodElement> methodSelector,
            AnnotationParser<S> parser,
            Object target
    ) {
        return new AnnotationReaderImpl<>(imperat, methodSelector, parser, target);
    }

    RootCommandClass<S> getRootClass();

    ClassElement getParsedClass();

    void acceptCommandsParsing(CommandClassVisitor<S, Set<Command<S>>> visitor);

    <E extends Throwable> void acceptThrowableResolversParsing(CommandClassVisitor<S, Set<MethodThrowableResolver<?, S>>> visitor);

}
