package studio.mevera.imperat.command.arguments;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.util.Preconditions;

public interface DefaultValueProvider {

    DefaultValueProvider EMPTY = new DefaultValueProvider() {
        @Override
        public @Nullable <S extends CommandSource> String provide(ExecutionContext<S> context, Argument<S> parameter) {
            return null;
        }
    };

    static DefaultValueProvider of(@NotNull String value) {
        Preconditions.notNull(value, "default cannot be null, use `DefaultValueProvider#empty` instead");
        return new DefaultValueProvider() {
            private final String cachedValue = value;
            @Override
            public <S extends CommandSource> @NotNull String provide(ExecutionContext<S> context, Argument<S> parameter) {
                return cachedValue;
            }
        };
    }

    static @NotNull DefaultValueProvider empty() {
        return EMPTY;
    }

    default boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Supplies a default-value for optional
     * usage parameters {@link Argument}
     *
     * @param context   the context
     * @param parameter the parameter
     * @return the resolved default value
     */
    @Nullable
    <S extends CommandSource> String provide(ExecutionContext<S> context, Argument<S> parameter);

}
