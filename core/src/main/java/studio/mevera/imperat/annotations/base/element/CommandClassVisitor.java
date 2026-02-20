package studio.mevera.imperat.annotations.base.element;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.CommandParsingMode;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
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

    public static <S extends Source> CommandClassVisitor<S, Set<Command<S>>> newCommandParsingVisitor(
        Imperat<S> imperat,
        AnnotationParser<S> parser
    ) {
        CommandParsingMode mode = imperat.config().getCommandParsingMode();

        return switch (mode) {
            case KOTLIN -> createKotlinVisitorReflectively(imperat, parser);
            case JAVA -> new CommandParsingVisitor<>(
                imperat,
                parser,
                ElementSelector.<MethodElement>create()
                    .addRule(MethodRules.HAS_KNOWN_SENDER)
                //.addRule(MethodRules.HAS_LEAST_ONLY_ONE_MAIN_ANNOTATION)
            );
        };
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

    public abstract R visitCommandClass(
            @NotNull ClassElement clazz
    );

    @SuppressWarnings("unchecked")
    private static <S extends Source> CommandClassVisitor<S, Set<Command<S>>> createKotlinVisitorReflectively(
        Imperat<S> imperat,
        AnnotationParser<S> parser
    ) {
        try {
            Class.forName("kotlin.coroutines.Continuation");
            Class.forName("kotlin.reflect.KFunction");
            Class<?> factory = Class.forName(
                "studio.mevera.imperat.annotations.base.element.KotlinCommandParsingVisitorFactory"
            );
            return (CommandClassVisitor<S, Set<Command<S>>>)
                factory
                    .getMethod("create", Imperat.class, AnnotationParser.class)
                    .invoke(null, imperat, parser);
        } catch (ReflectiveOperationException | NoClassDefFoundError e) {
            throw new IllegalStateException(
                "Kotlin support requested but Kotlin runtime is unavailable, make sure you've also added kotlin-reflect.",
                e
            );
        }
    }

}
