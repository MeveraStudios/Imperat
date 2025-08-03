package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Preconditions;

public interface OptionalValueSupplier {

    OptionalValueSupplier EMPTY = new OptionalValueSupplier() {
        @Override
        public @Nullable <S extends Source> String supply(S source, CommandParameter<S> parameter) {
            return null;
        }
    };

    static  OptionalValueSupplier of(@NotNull String value) {
        Preconditions.notNull(value, "default cannot be null, use `OptionalValueSupplier#empty` instead");
        return new OptionalValueSupplier() {
            @Override
            public <S extends Source> @NotNull String supply(S source, CommandParameter<S> parameter) {
                return value;
            }
        };
    }

    static @NotNull OptionalValueSupplier empty() {
        return EMPTY;
    }

    default boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Supplies a default-value for optional
     * usage parameters {@link CommandParameter}
     *
     * @param source the context
     * @param parameter the parameter
     * @return the resolved default value
     */
    @Nullable
    <S extends Source> String supply(S source, CommandParameter<S> parameter);

}
