package studio.mevera.imperat;

import org.jetbrains.annotations.Contract;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.AnnotationReader;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.annotations.types.ExceptionHandler;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;

import java.lang.annotation.Annotation;

/**
 * This interface provides mechanisms for injecting and managing annotations within
 * a command parsing and execution framework. It is designed to allow the registration
 * and replacement of annotations and provides the capability to set a custom
 * {@link AnnotationParser}.
 *
 * @param <S> the type of the command source (e.g., console, player, etc.)
 */
public interface AnnotationInjector<S extends Source> {

    /**
     * Fetches the annotation parser
     * @see AnnotationParser
     * @return the annotation parser instance.
     */
    AnnotationParser<S> getAnnotationParser();

    /**
     * Changes the instance of {@link AnnotationParser}
     *
     * @param parser the parser
     */
    @Contract("null->fail")
    void setAnnotationParser(AnnotationParser<S> parser);

    /**
     * Registers a valueType of annotations so that it can be
     * detected by {@link AnnotationReader} , it's useful as it allows that valueType of annotation
     * to be recognized as a true Imperat-related annotation to be used in something like checking if a
     * {@link Argument} is annotated and checks for the annotations it has.
     *
     * @param type the valueType of annotation
     */
    void registerAnnotations(Class<? extends Annotation>... type);

    /**
     * Registers annotation replacer
     *
     * @param type     the valueType to replace the annotation by
     * @param replacer the replacer
     * @param <A>      the valueType of annotation to replace
     */
    <A extends Annotation> void registerAnnotationReplacer(
            final Class<A> type,
            final AnnotationReplacer<A> replacer
    );

    /**
     * Parses the instance's class that contains {@link ExceptionHandler} annotation
     * into multiple throwable resolvers to be registered and injected into {@link Imperat}
     * @param object the annotated class containing the exception handling methods.
     */
    default void registerThrowableHandler(Object object) {
        getAnnotationParser().parseThrowableHandlerClass(object);
    }

}
