package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ParameterTypes {

    private ParameterTypes() {
    }

    public static <S extends Source> ParameterString<S> string() {
        return new ParameterString<>();
    }

    public static <S extends Source, N extends Number> ParameterNumber<S, N> numeric(Class<N> numType) {
        return ParameterNumber.from(numType);
    }

    public static <S extends Source> ParameterBoolean<S> bool() {
        return new ParameterBoolean<>();
    }

    public static <S extends Source> ParameterFlag<S> flag(FlagData<S> flagData) {
        return new ParameterFlag<>(flagData);
    }


    public static @NotNull <S extends Source> ParameterCommand<S> command(String name, List<String> aliases) {
        return new ParameterCommand<>(name, aliases);
    }

    public static <S extends Source> ParameterUUID<S> uuid() {
        return new ParameterUUID<>();
    }

    public static <S extends Source, E> ParameterArray<S, E> array(
            TypeWrap<E[]> type,
            Function<Integer, Object[]> initializer,
            ParameterType<S, E> componentType
    ) {
        return new ParameterArray<>(type, initializer, componentType) {
        };
    }

    public static <S extends Source, E, C extends Collection<E>> ParameterCollection<S, E, C> collection(
            TypeWrap<C> type,
            Supplier<C> collectionSupplier,
            ParameterType<S, E> componentResolver
    ) {
        return new ParameterCollection<>(type, collectionSupplier, componentResolver);
    }

    public static <S extends Source, K, V, M extends Map<K, V>> ParameterMap<S, K, V, M> map(
            TypeWrap<M> type,
            Supplier<M> mapInitializer,
            ParameterType<S, K> keyResolver,
            ParameterType<S, V> valueResolver
    ) {
        return new ParameterMap<>(type, mapInitializer, keyResolver, valueResolver);
    }


    public static <S extends Source, T> ParameterCompletableFuture<S, T> future(
            TypeWrap<CompletableFuture<T>> typeWrap,
            ParameterType<S, T> resolverType
    ) {
        return new ParameterCompletableFuture<>(typeWrap, resolverType);
    }

    public static <S extends Source, T> ParameterOptional<S, T> optional(
            TypeWrap<Optional<T>> typeWrap,
            ParameterType<S, T> resolverType
    ) {
        return new ParameterOptional<>(typeWrap, resolverType);
    }
}
