package studio.mevera.imperat.config.registries;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.AnnotationInjector;
import studio.mevera.imperat.annotations.base.AnnotationReplacer;
import studio.mevera.imperat.context.CommandSource;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores {@link AnnotationReplacer}s registered through the config builder
 * before an {@link AnnotationInjector} (typically the {@code Imperat} instance)
 * exists, then forwards them to that injector at construction time.
 *
 * <p>Order of registration is preserved by using a {@link LinkedHashMap}. The
 * bridging method {@link #installInto(AnnotationInjector)} is package-private
 * to {@code studio.mevera.imperat.config.registries} but is exposed via
 * {@code ImperatConfigImpl} (which delegates) and called by {@code BaseImperat}'s
 * constructor — users do not invoke it directly.</p>
 */
public final class AnnotationReplacerRegistry {

    private final Map<Class<? extends Annotation>, AnnotationReplacer<?>> replacers = new LinkedHashMap<>();

    public <A extends Annotation> void register(@NotNull Class<A> type, @NotNull AnnotationReplacer<A> replacer) {
        replacers.put(type, replacer);
    }

    public boolean isEmpty() {
        return replacers.isEmpty();
    }

    public int size() {
        return replacers.size();
    }

    /**
     * Replays every registered replacer onto {@code injector}. Used during
     * {@code Imperat} construction so replacers staged on the config before
     * the parser exists land on the parser once it does.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <S extends CommandSource> void installInto(@NotNull AnnotationInjector<S> injector) {
        replacers.forEach((type, replacer) -> {
            Class annType = type;
            AnnotationReplacer annReplacer = replacer;
            injector.registerAnnotationReplacer(annType, annReplacer);
        });
    }
}
