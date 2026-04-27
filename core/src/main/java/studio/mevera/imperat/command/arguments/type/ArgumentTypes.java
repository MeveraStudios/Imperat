package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Either;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ArgumentTypes {

    private ArgumentTypes() {
    }

    public static <S extends CommandSource> StringArgument<S> string() {
        return new StringArgument<>();
    }

    public static <S extends CommandSource> CharacterArgument<S> character() {
        return new CharacterArgument<>();
    }

    public static <S extends CommandSource, N extends Number> NumberArgument<S, N> numeric(Class<N> numType) {
        return NumberArgument.from(numType);
    }

    public static <S extends CommandSource> BooleanArgument<S> bool() {
        return new BooleanArgument<>();
    }

    public static <S extends CommandSource> FlagArgumentType<S> flag(FlagData<S> flagData) {
        return new FlagArgumentType<>(flagData);
    }


    public static @NotNull <S extends CommandSource> CommandArgument<S> command(Command<S> command) {
        return new CommandArgument<>(command);
    }

    public static <S extends CommandSource> UUIDArgument<S> uuid() {
        return new UUIDArgument<>();
    }

    public static <S extends CommandSource> DurationArgument<S> duration() {
        return new DurationArgument<>();
    }

    public static <S extends CommandSource> InstantArgument<S> instant() {
        return new InstantArgument<>();
    }

    public static <S extends CommandSource> LocalDateArgument<S> localDate() {
        return new LocalDateArgument<>();
    }

    public static <S extends CommandSource> LocalDateTimeArgument<S> localDateTime() {
        return new LocalDateTimeArgument<>();
    }

    public static <S extends CommandSource> BigDecimalArgument<S> bigDecimal() {
        return new BigDecimalArgument<>();
    }

    public static <S extends CommandSource> BigIntegerArgument<S> bigInteger() {
        return new BigIntegerArgument<>();
    }

    public static <S extends CommandSource> PathArgument<S> path() {
        return new PathArgument<>();
    }

    public static <S extends CommandSource> PatternArgument<S> regex() {
        return new PatternArgument<>();
    }

    public static <S extends CommandSource> URIArgument<S> uri() {
        return new URIArgument<>();
    }

    public static <S extends CommandSource, E> ArrayArgument<S, E> array(
            TypeWrap<E[]> type,
            Function<Integer, Object[]> initializer,
            ArgumentType<S, E> componentType
    ) {
        return new ArrayArgument<>(type, initializer, componentType) {
        };
    }

    public static <S extends CommandSource, E, C extends Collection<E>> CollectionArgument<S, E, C> collection(
            TypeWrap<C> type,
            Supplier<C> collectionSupplier,
            ArgumentType<S, E> componentResolver
    ) {
        return new CollectionArgument<>(type, collectionSupplier, componentResolver);
    }

    public static <S extends CommandSource, K, V, M extends Map<K, V>> MapArgument<S, K, V, M> map(
            TypeWrap<M> type,
            Supplier<M> mapInitializer,
            ArgumentType<S, K> keyResolver,
            ArgumentType<S, V> valueResolver
    ) {
        return new MapArgument<>(type, mapInitializer, keyResolver, valueResolver);
    }


    public static <S extends CommandSource, T> CompletableFutureArgument<S, T> future(
            TypeWrap<CompletableFuture<T>> typeWrap,
            ArgumentType<S, T> resolverType
    ) {
        return new CompletableFutureArgument<>(typeWrap, resolverType);
    }

    public static <S extends CommandSource, T> OptionalArgument<S, T> optional(
            TypeWrap<Optional<T>> typeWrap,
            ArgumentType<S, T> resolverType
    ) {
        return new OptionalArgument<>(typeWrap, resolverType);
    }

    public static <S extends CommandSource, A, B> EitherArgument<S, A, B> either(
        TypeWrap<Either<A, B>> typeWrap,
        TypeWrap<A> typeA,
        TypeWrap<B> typeB
    ) {
        return new EitherArgument<>(typeWrap, typeA, typeB);
    }
}
