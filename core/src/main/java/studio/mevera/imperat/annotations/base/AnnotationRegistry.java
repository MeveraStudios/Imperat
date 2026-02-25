package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.ArgType;
import studio.mevera.imperat.annotations.Async;
import studio.mevera.imperat.annotations.ContextResolved;
import studio.mevera.imperat.annotations.Cooldown;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.ExceptionHandler;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.ExternalSubCommand;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Format;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.InheritedArg;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.PostProcessor;
import studio.mevera.imperat.annotations.PreProcessor;
import studio.mevera.imperat.annotations.Range;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.Shortcut;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Suggest;
import studio.mevera.imperat.annotations.SuggestionProvider;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.Validators;
import studio.mevera.imperat.annotations.Values;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AnnotationRegistry {

    private final Set<Class<? extends Annotation>> knownAnnotations = new LinkedHashSet<>();

    private final Map<Class<? extends Annotation>, AnnotationReplacer<?>> replacers = new HashMap<>();

    AnnotationRegistry() {
        this.registerAnnotationTypes(
                RootCommand.class, ExternalSubCommand.class, Execute.class, SubCommand.class,
                Cooldown.class, Description.class, Permission.class, Format.class, Shortcut.class,
                Suggest.class, SuggestionProvider.class, Default.class, DefaultProvider.class, Values.class,
                Switch.class, Flag.class, Greedy.class, Named.class, Optional.class, ContextResolved.class, Range.class, Async.class,
                PostProcessor.class, PreProcessor.class, ExceptionHandler.class,
                Validators.class, ArgType.class, InheritedArg.class
        );
    }

    private static boolean isRegistered(Class<? extends Annotation> annotationClass,
            Collection<Class<? extends Annotation>> annotations) {
        for (Class<? extends Annotation> aC : annotations) {
            if (aC.getName().equals(annotationClass.getName())) {
                return true;
            }
        }
        return false;
    }

    <A extends Annotation> void registerAnnotationReplacer(Class<A> type, AnnotationReplacer<A> replacer) {
        this.replacers.put(type, replacer);
    }

    @SuppressWarnings("unchecked")
    <A extends Annotation> @Nullable AnnotationReplacer<A> getAnnotationReplacer(Class<A> type) {
        return (AnnotationReplacer<A>) this.replacers.get(type);
    }

    boolean hasReplacerFor(Class<? extends Annotation> clazz) {
        return getAnnotationReplacer(clazz) != null;
    }

    @SafeVarargs final void registerAnnotationTypes(Class<? extends Annotation>... annotationClasses) {
        knownAnnotations.addAll(List.of(annotationClasses));
    }

    boolean isRegisteredAnnotation(Class<? extends Annotation> annotationClass) {
        return isRegistered(annotationClass, knownAnnotations);
    }


}
