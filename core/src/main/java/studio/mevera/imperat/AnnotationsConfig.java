package studio.mevera.imperat;

import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.context.CommandSource;

import java.lang.annotation.Annotation;

public final class AnnotationsConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    AnnotationsConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public <A extends Annotation> AnnotationsConfig<S> replacer(Class<A> annotationType, AnnotationReplacer<A> replacer) {
        config.registerAnnotationReplacer(annotationType, replacer);
        return this;
    }
}
