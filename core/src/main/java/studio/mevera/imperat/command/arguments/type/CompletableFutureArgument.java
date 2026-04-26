package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.DefaultValueProvider;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.TypeWrap;

import java.util.concurrent.CompletableFuture;

public final class CompletableFutureArgument<S extends CommandSource, T> extends ArgumentType<S, CompletableFuture<T>> {

    private final ArgumentType<S, T> typeResolver;

    public CompletableFutureArgument(TypeWrap<CompletableFuture<T>> typeWrap, ArgumentType<S, T> typeResolver) {
        super(typeWrap.getType());
        this.typeResolver = typeResolver;
    }

    @Override
    public CompletableFuture<T> parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) {
        if (typeResolver == null) {
            throw new IllegalStateException("No type parameter for type '" + type.getTypeName() + "'");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return typeResolver.parse(context, argument, input);
            } catch (Exception ex) {
                context.imperatConfig().handleExecutionError(ex, context, CompletableFutureArgument.class, "parse");
                return null;
            }
        });
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
    public int getNumberOfParametersToConsume(Argument<S> argument) {
        return typeResolver.getNumberOfParametersToConsume(argument);
    }

}
