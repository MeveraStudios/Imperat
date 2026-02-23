package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.TypeWrap;

import java.util.concurrent.CompletableFuture;

public final class CompletableFutureArgument<S extends Source, T> extends ArgumentType<S, CompletableFuture<T>> {

    private final ArgumentType<S, T> typeResolver;

    public CompletableFutureArgument(TypeWrap<CompletableFuture<T>> typeWrap, ArgumentType<S, T> typeResolver) {
        super(typeWrap.getType());
        this.typeResolver = typeResolver;
    }

    @Override
    public @NotNull CompletableFuture<@Nullable T> parse(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput) throws CommandException {

        if (typeResolver == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No type parameter for type '" + type.getTypeName() + "'")
            );
        }
        Cursor<S> copyStream = cursor.copy();
        //CommandInputStream<S> singleStream = CommandInputStream.ofSingleString(inputStream.currentParameter().orElseThrow(), input);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return typeResolver.parse(context, copyStream, correspondingInput);
            } catch (CommandException e) {
                context.imperatConfig()
                        .handleExecutionThrowable(e, context, CompletableFutureArgument.class, "resolve");
                return null;
            }
        });
    }

    @Override
    public SuggestionResolver<S> getSuggestionResolver() {
        return typeResolver.getSuggestionResolver();
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> context, Argument<S> parameter) {
        return typeResolver.matchesInput(rawPosition, context, parameter);
    }

    @Override
    public DefaultValueProvider getDefaultValueProvider() {
        return typeResolver.getDefaultValueProvider();
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return typeResolver.isGreedy(parameter);
    }

}
