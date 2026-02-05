package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Either;
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

public final class ArgumentTypes {

    private ArgumentTypes() {
    }

    public static <S extends Source> StringArgument<S> string() {
        return new StringArgument<>();
    }

    public static <S extends Source, N extends Number> NumberArgument<S, N> numeric(Class<N> numType) {
        return NumberArgument.from(numType);
    }

    public static <S extends Source> BooleanArgument<S> bool() {
        return new BooleanArgument<>();
    }

    public static <S extends Source> FlagArgumentType<S> flag(FlagData<S> flagData) {
        return new FlagArgumentType<>(flagData);
    }


    public static @NotNull <S extends Source> CommandArgument<S> command(String name, List<String> aliases) {
        return new CommandArgument<>(name, aliases);
    }

    public static <S extends Source> UUIDArgument<S> uuid() {
        return new UUIDArgument<>();
    }

    public static <S extends Source, E> ArrayArgument<S, E> array(
            TypeWrap<E[]> type,
            Function<Integer, Object[]> initializer,
            ArgumentType<S, E> componentType
    ) {
        return new ArrayArgument<>(type, initializer, componentType) {
        };
    }

    public static <S extends Source, E, C extends Collection<E>> CollectionArgument<S, E, C> collection(
            TypeWrap<C> type,
            Supplier<C> collectionSupplier,
            ArgumentType<S, E> componentResolver
    ) {
        return new CollectionArgument<>(type, collectionSupplier, componentResolver);
    }

    public static <S extends Source, K, V, M extends Map<K, V>> MapArgument<S, K, V, M> map(
            TypeWrap<M> type,
            Supplier<M> mapInitializer,
            ArgumentType<S, K> keyResolver,
            ArgumentType<S, V> valueResolver
    ) {
        return new MapArgument<>(type, mapInitializer, keyResolver, valueResolver);
    }


    public static <S extends Source, T> CompletableFutureArgument<S, T> future(
            TypeWrap<CompletableFuture<T>> typeWrap,
            ArgumentType<S, T> resolverType
    ) {
        return new CompletableFutureArgument<>(typeWrap, resolverType);
    }

    public static <S extends Source, T> OptionalArgument<S, T> optional(
            TypeWrap<Optional<T>> typeWrap,
            ArgumentType<S, T> resolverType
    ) {
        return new OptionalArgument<>(typeWrap, resolverType);
    }

    public static <S extends Source, A, B> EitherArgument<S, A, B> either(
        TypeWrap<Either<A, B>> typeWrap,
        TypeWrap<A> typeA,
        TypeWrap<B> typeB
    ) {
        return new EitherArgument<>(typeWrap, typeA, typeB);
    }
}
