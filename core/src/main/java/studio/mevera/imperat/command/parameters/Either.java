package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public final class Either<A, B> {

    private final A primary;
    private final B fallback;

    private Either(A primary, B b) {
        this.primary = primary;
        this.fallback = b;
    }

    public static <A, B> Either<A, B> ofPrimary(A primary) {
        return new Either<>(primary, null);
    }

    public static <A, B> Either<A, B> ofFallback(B fallback) {
        return new Either<>(null, fallback);
    }

    public A primaryOrMapFallback(final @NotNull Function<B, A> mapFallback) {
        return this.getPrimary().orElseGet(() -> mapFallback.apply(this.getFallback().orElseThrow()));
    }

    public B fallbackOrMapPrimary(final @NotNull Function<A, B> mapPrimary) {
        return this.getFallback().orElseGet(() -> mapPrimary.apply(this.getPrimary().orElseThrow()));
    }

    public Optional<A> getPrimary() {
        return Optional.ofNullable(primary);
    }

    public Optional<B> getFallback() {
        return Optional.ofNullable(fallback);
    }

}
