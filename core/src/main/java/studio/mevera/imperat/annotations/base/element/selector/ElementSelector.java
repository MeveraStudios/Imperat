package studio.mevera.imperat.annotations.base.element.selector;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.element.ParseElement;

import java.util.List;

/**
 * Selects a {@link ParseElement} based on a specific list of {@link Rule}
 *
 * @param <E> the valueType of {@link ParseElement} to select
 */
public sealed interface ElementSelector<E extends ParseElement<?>> permits SimpleElementSelector {

    /**
     * @return the rules that all must be followed to select a single {@link ParseElement}
     * * during annotation parsing.
     * @see Rule
     */
    @NotNull
    List<Rule<E>> getRules();

    default void verifyWithFail(Rule<E> rule, Imperat<?> imperat, AnnotationParser<?> registry, E element) {
        if (!rule.test(imperat, registry, element)) {
            rule.onFailure(registry, element);
        }
    }

    default ElementSelector<E> addRule(Rule<E> rule) {
        getRules().add(rule);
        return this;
    }

    default void removeRule(Rule<E> rule) {
        getRules().remove(rule);
    }

    default boolean canBeSelected(Imperat<?> imperat, AnnotationParser<?> parse, E element, boolean fail) {
        for (var rule : getRules()) {
            
            if (!rule.test(imperat, parse, element)) {
                System.out.println("RULE FAILED");
                if (fail) {
                    rule.onFailure(parse, element);
                }
                return false;
            }else {
                System.out.println("RULE SUCCESS");
            }
        }
        return true;
    }

    static <E extends ParseElement<?>> ElementSelector<E> create() {
        return new SimpleElementSelector<>();
    }

}
