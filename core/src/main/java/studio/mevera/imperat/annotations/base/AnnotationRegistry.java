package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.types.ArgType;
import studio.mevera.imperat.annotations.types.Async;
import studio.mevera.imperat.annotations.types.Context;
import studio.mevera.imperat.annotations.types.Cooldown;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.DefaultProvider;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.ExceptionHandler;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.ExternalSubCommand;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Format;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.InheritedArg;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.Permission;
import studio.mevera.imperat.annotations.types.PostProcessor;
import studio.mevera.imperat.annotations.types.PreProcessor;
import studio.mevera.imperat.annotations.types.Range;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Shortcut;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Suggest;
import studio.mevera.imperat.annotations.types.SuggestionProvider;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.annotations.types.Validators;
import studio.mevera.imperat.annotations.types.Values;

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
                Switch.class, Flag.class, Greedy.class, Named.class, Optional.class, Context.class, Range.class, Async.class,
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
