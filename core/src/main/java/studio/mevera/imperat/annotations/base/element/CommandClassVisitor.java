package studio.mevera.imperat.annotations.base.element;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.annotations.base.element.selector.MethodRules;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;

import java.util.Set;

/**
 * Visits each element in a {@link ClassElement}
 *
 * @param <S> the command source
 */
public abstract class CommandClassVisitor<S extends Source, R> {
    public final static ElementSelector<MethodElement> ERROR_HANDLING_METHOD_SELECTOR =
            ElementSelector.<MethodElement>create()
                    .addRule(MethodRules.IS_PUBLIC)
                    .addRule(MethodRules.HAS_EXCEPTION_HANDLER_ANNOTATION)
                    .addRule(MethodRules.HAS_EXCEPTION_HANDLER_PARAMS_IN_ORDER);

    protected final Imperat<S> imperat;
    protected final AnnotationParser<S> parser;
    protected final ElementSelector<MethodElement> methodSelector;
    protected CommandClassVisitor(
        Imperat<S> imperat,
        AnnotationParser<S> parser,
        ElementSelector<MethodElement> methodSelector
    ) {
        this.imperat = imperat;
        this.parser = parser;
        this.methodSelector = methodSelector;
    }
    
    
    public abstract R visitCommandClass(
        @NotNull ClassElement clazz
    );

    public static <S extends Source> CommandClassVisitor<S, Set<Command<S>>> newCommandParsingVisitor(
        Imperat<S> imperat,
        AnnotationParser<S> parser
    ) {
        return new CommandParsingVisitor<>(
            imperat,
            parser,
            ElementSelector.<MethodElement>create()
                .addRule(MethodRules.HAS_KNOWN_SENDER)
                //.addRule(MethodRules.HAS_LEAST_ONLY_ONE_MAIN_ANNOTATION)
        );
    }
    
    public static <S extends Source> CommandClassVisitor<S, Set<MethodThrowableResolver<?, S>>> newThrowableParsingVisitor(
            Imperat<S> imperat,
            AnnotationParser<S> parser
    ) {
        return new ThrowableParsingVisitor<>(
                imperat,
                parser,
                ERROR_HANDLING_METHOD_SELECTOR
        );
    }
    
}
