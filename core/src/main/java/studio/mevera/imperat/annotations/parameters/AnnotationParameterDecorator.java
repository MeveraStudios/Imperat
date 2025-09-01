package studio.mevera.imperat.annotations.parameters;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.command.parameters.InputParameter;
import studio.mevera.imperat.context.Source;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

public final class AnnotationParameterDecorator<S extends Source> extends InputParameter<S> implements AnnotatedParameter<S> {

    private final CommandParameter<S> parameter;
    private final ParameterElement element;

    AnnotationParameterDecorator(CommandParameter<S> parameter, ParameterElement element) {
        super(
            parameter.name(), parameter.type(), parameter.getSinglePermission(),
            parameter.description(), parameter.isOptional(),
            parameter.isFlag(), parameter.isGreedy(),
            parameter.getDefaultValueSupplier(), parameter.getSuggestionResolver()
        );
        this.parameter = parameter;
        this.element = element;
    }

    public static <S extends Source> AnnotationParameterDecorator<S> decorate(
        CommandParameter<S> parameter,
        ParameterElement element
    ) {
        return new AnnotationParameterDecorator<>(parameter, element);
    }

    /**
     * Get the instance of specific annotation
     *
     * @param clazz the valueType of annotation
     * @return the specific instance of an annotation of a certain valueType {@linkplain A}
     */
    @Override
    public <A extends Annotation> @Nullable A getAnnotation(Class<A> clazz) {
        return element.getAnnotation(clazz);
    }

    /**
     * @return the annotations associated with this parameter
     */
    @Override
    @Unmodifiable
    public Collection<? extends Annotation> getAnnotations() {
        return List.of(element.getAnnotations());
    }

    /**
     * Formats the usage parameter*
     *
     * @return the formatted parameter
     */
    @Override
    public String format() {
        return parameter.format();
    }

    /**
     * Casts the parameter to a flag parameter
     *
     * @return the parameter as a flag
     */
    @Override
    public FlagParameter<S> asFlagParameter() {
        return parameter.asFlagParameter();
    }

    /**
     * @return checks whether this parameter is a flag
     */
    @Override
    public boolean isFlag() {
        return parameter.isFlag();
    }

    /**
     * Creates a copy of this parameter with a different position.
     * Useful for commands that have multiple syntaxes.
     *
     * @param newPosition the new position to set
     * @return a copy of this parameter with the new position
     */
    @Override
    public CommandParameter<S> copyWithDifferentPosition(int newPosition) {
        CommandParameter<S> copiedParameter = parameter.copyWithDifferentPosition(newPosition);
        AnnotationParameterDecorator<S> copy = new AnnotationParameterDecorator<>(
            copiedParameter,
            this.element
        );
        copy.setFormat(this.format);
        return copy;
    }

}
