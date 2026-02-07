package studio.mevera.imperat.annotations.base.element.selector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.element.MethodElement;

import java.util.function.BiConsumer;

@SuppressWarnings("rawtypes")
public sealed interface Rule<T> permits Rule.SimpleRule {

    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    static Builder<MethodElement> buildForMethod() {
        return new Builder<>();
    }

    @NotNull
    RuleCondition<T> condition();

    default boolean test(Imperat imperat, AnnotationParser<?> registry, T variable) {
        return condition().test(imperat, registry, variable);
    }

    void onFailure(AnnotationParser<?> registry, T variable);

    @NotNull
    Rule<T> and(@Nullable Rule<T> other);

    @NotNull
    Rule<T> or(@Nullable Rule<T> other);

    class Builder<T> {

        RuleCondition<T> condition;
        BiConsumer<AnnotationParser<?>, T> runnable;

        Builder() {

        }

        public Builder<T> condition(RuleCondition<T> condition) {
            this.condition = condition;
            return this;
        }

        public Builder<T> failure(BiConsumer<AnnotationParser<?>, T> runnable) {
            this.runnable = runnable;
            return this;
        }

        public Rule<T> build() {
            return new SimpleRule<>(condition, runnable);
        }

    }

    non-sealed class SimpleRule<T> implements Rule<T> {

        private final @NotNull BiConsumer<AnnotationParser<?>, T> onFailure;
        private @NotNull RuleCondition<T> condition;

        public SimpleRule(@NotNull RuleCondition<T> condition, @NotNull BiConsumer<AnnotationParser<?>, T> onFailure) {
            this.condition = condition;
            this.onFailure = onFailure;
        }

        @Override
        public void onFailure(AnnotationParser<?> registry, T variable) {
            onFailure.accept(registry, variable);
        }

        @Override
        public @NotNull Rule<T> and(@Nullable Rule<T> other) {
            if (other == null) {
                return this;
            }
            condition = condition.and(other.condition());
            return this;
        }

        @Override
        public @NotNull Rule<T> or(@Nullable Rule<T> other) {
            if (other == null) {
                return this;
            }
            condition = condition.or(other.condition());
            return this;
        }

        @Override
        public @NotNull RuleCondition<T> condition() {
            return condition;
        }

    }
}
