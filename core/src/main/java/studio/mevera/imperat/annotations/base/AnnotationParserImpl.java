package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.CommandClassVisitor;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.MethodThrowableResolver;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.annotations.base.element.selector.MethodRules;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;

import java.lang.annotation.Annotation;
import java.util.Set;

@ApiStatus.Internal
final class AnnotationParserImpl<S extends Source> extends AnnotationParser<S> {

    final AnnotationRegistry annotationRegistry;
    private final ElementSelector<MethodElement> methodSelector;
    private final CommandClassVisitor<S, Set<Command<S>>> commandParsingVisitor;
    private final CommandClassVisitor<S, Set<MethodThrowableResolver<?, S>>> throwableHandlerVisitor;

    AnnotationParserImpl(Imperat<S> dispatcher) {
        super(dispatcher);
        this.annotationRegistry = new AnnotationRegistry();

        this.methodSelector = ElementSelector.create();
        methodSelector.addRule(
                MethodRules.IS_PUBLIC/*.and(MethodRules.RETURNS_VOID)*/.and(MethodRules.HAS_A_MAIN_ANNOTATION)
        );

        this.commandParsingVisitor = CommandClassVisitor.newCommandParsingVisitor(dispatcher, this);
        this.throwableHandlerVisitor = CommandClassVisitor.newThrowableParsingVisitor(dispatcher, this);
    }


    @Override
    public <T> void parseCommandClass(T instance) {
        //loading dependency
        AnnotationReader<S> reader = AnnotationReader.read(imperat, methodSelector, this, instance);
        reader.acceptCommandsParsing(commandParsingVisitor);
    }

    @Override
    public <T> void parseThrowableHandlerClass(T instance) {
        AnnotationReader<S> reader = AnnotationReader.read(imperat, CommandClassVisitor.ERROR_HANDLING_METHOD_SELECTOR, this, instance);
        reader.acceptThrowableResolversParsing(throwableHandlerVisitor);
    }


    /**
     * Registers a valueType of annotations so that it can be
     * detected by {@link AnnotationReader} , it's useful as it allows that valueType of annotation
     * to be recognized as a true Imperat-related annotation to be used in something like checking if a
     * {@link Argument} is annotated and checks for the annotations it has.
     *
     * @param type the valueType of annotation
     */
    @SafeVarargs
    @Override
    public final void registerAnnotations(Class<? extends Annotation>... type) {
        annotationRegistry.registerAnnotationTypes(type);
    }


    /**
     * Registers {@link AnnotationReplacer}
     *
     * @param type     the valueType to replace the annotation by
     * @param replacer the replacer
     */
    @Override
    public <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer) {
        annotationRegistry.registerAnnotationReplacer(type, replacer);
    }

    /**
     * Checks the internal registry whether the valueType of annotation entered is known/registered or not.
     *
     * @param annotationType the valueType of annotation to enter
     * @return whether the valueType of annotation entered is known/registered or not.
     */
    @Override
    public boolean isKnownAnnotation(Class<? extends Annotation> annotationType) {
        return annotationRegistry.isRegisteredAnnotation(annotationType);
    }

    /**
     * Checks if the specific valueType of annotation entered has a {@link AnnotationReplacer}
     * for it in the internal registry for replacers
     *
     * @param type the valueType of annotation entered
     * @return Whether there's an annotation replacer for the valueType entered.
     */
    @Override
    public boolean hasAnnotationReplacerFor(Class<? extends Annotation> type) {
        return annotationRegistry.hasReplacerFor(type);
    }

    /**
     * Fetches the {@link AnnotationReplacer} mapped to the entered annotation valueType.
     *
     * @param type the valueType of annotation
     * @return the {@link AnnotationReplacer} mapped to the entered annotation valueType.
     */
    @Override
    public <A extends Annotation> @Nullable AnnotationReplacer<A> getAnnotationReplacer(Class<A> type) {
        return annotationRegistry.getAnnotationReplacer(type);
    }

    @Override AnnotationRegistry getAnnotationRegistry() {
        return annotationRegistry;
    }

}
