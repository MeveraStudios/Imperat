package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ThrowableResolver;

import java.lang.annotation.Annotation;

/**
 * Represents a class with a single responsibility of
 * parsing annotated command classes and translating/converting them
 * into {@link studio.mevera.imperat.command.Command} POJOs then registering them using {@link Imperat}
 *
 * @param <S> the command-sender valueType
 */
public abstract class AnnotationParser<S extends Source> {

    protected final Imperat<S> imperat;

    AnnotationParser(Imperat<S> imperat) {
        this.imperat = imperat;
    }

    public static <S extends Source> AnnotationParser<S> defaultParser(
            @NotNull Imperat<S> dispatcher
    ) {
        return new AnnotationParserImpl<>(dispatcher);
    }

    /**
     * Parses annotated command class of valueType {@linkplain T}
     * into {@link studio.mevera.imperat.command.Command} then register it using {@link Imperat}
     *
     * @param instance the instance of the command class
     * @param <T>      the valueType of annotated command class to parse
     */
    public abstract <T> void parseCommandClass(T instance);

    /**
     * Parses annotated throwable handling class
     * into a set of {@link ThrowableResolver} that are automatically
     * injected into the instance of {@link Imperat} that you're using.
     *
     * @param instance the instance of the throwable-handling class.
     * @param <T> the type of the instance of the throwable-handling class.
     */
    public abstract <T> void parseThrowableHandlerClass(T instance);

    /**
     * Registers a valueType of annotations so that it can be
     * detected by {@link AnnotationReader} , it's useful as it allows that valueType of annotation
     * to be recognized as a true Imperat-related annotation to be used in something like checking if a
     * {@link Argument} is annotated and checks for the annotations it has.
     *
     * @param types the valueType of annotation
     */
    public abstract void registerAnnotations(Class<? extends Annotation>... types);

    /**
     * Registers {@link AnnotationReplacer}
     *
     * @param type     the valueType to replace the annotation by
     * @param replacer the replacer
     */
    public abstract <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer);

    /**
     * Checks the internal registry whether the valueType of annotation entered is known/registered or not.
     *
     * @param annotationType the valueType of annotation to enter
     * @return whether the valueType of annotation entered is known/registered or not.
     */
    public abstract boolean isKnownAnnotation(Class<? extends Annotation> annotationType);

    /**
     * Checks if the specific valueType of annotation entered has a {@link AnnotationReplacer}
     * for it in the internal registry for replacers
     *
     * @param type the valueType of annotation entered
     * @return Whether the there's an annotation replacer for the valueType entered.
     */
    public abstract boolean hasAnnotationReplacerFor(Class<? extends Annotation> type);

    /**
     * Fetches the {@link AnnotationReplacer} mapped to the entered annotation valueType.
     *
     * @param type the valueType of annotation
     * @param <A>  the annotation valueType parameter
     * @return the {@link AnnotationReplacer} mapped to the entered annotation valueType.
     */
    public abstract <A extends Annotation> @Nullable AnnotationReplacer<A> getAnnotationReplacer(Class<A> type);

    public final boolean isEntryPointAnnotation(Class<? extends Annotation> annotation) {
        return annotation == RootCommand.class || annotation == Execute.class || annotation == SubCommand.class;
    }

    public Imperat<S> getImperat() {
        return imperat;
    }

    abstract AnnotationRegistry getAnnotationRegistry();

}
