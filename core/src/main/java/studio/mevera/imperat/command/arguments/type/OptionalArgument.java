package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.DefaultValueProvider;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Optional;

public final class OptionalArgument<S extends CommandSource, T> extends ArgumentType<S, Optional<T>> {

    private final ArgumentType<S, T> typeResolver;

    public OptionalArgument(TypeWrap<Optional<T>> typeWrap, ArgumentType<S, T> typeResolver) {
        super(typeWrap.getType());
        this.typeResolver = typeResolver;
    }

    @Override
    public SuggestionProvider<S> getSuggestionProvider() {
        return typeResolver.getSuggestionProvider();
    }

    @Override
    public DefaultValueProvider getDefaultValueProvider() {
        return typeResolver.getDefaultValueProvider();
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return typeResolver.isGreedy(parameter);
    }

    @Override
    public Optional<T> parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException, ResponseException {
        T value = typeResolver.parse(context, argument, input);
        return Optional.ofNullable(value);
    }

    @Override
    public Optional<T> parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws CommandException, ResponseException {
        T value = typeResolver.parse(context, cursor);
        return Optional.ofNullable(value);
    }
}
