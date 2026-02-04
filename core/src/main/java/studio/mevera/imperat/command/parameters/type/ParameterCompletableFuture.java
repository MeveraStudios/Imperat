package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.TypeWrap;

import java.util.concurrent.CompletableFuture;

public final class ParameterCompletableFuture<S extends Source, T> extends BaseParameterType<S, CompletableFuture<T>> {

    private final ParameterType<S, T> typeResolver;

    public ParameterCompletableFuture(TypeWrap<CompletableFuture<T>> typeWrap, ParameterType<S, T> typeResolver) {
        super(typeWrap.getType());
        this.typeResolver = typeResolver;
    }

    @Override
    public @NotNull CompletableFuture<@Nullable T> resolve(
            @NotNull ExecutionContext<S> context,
            @NotNull CommandInputStream<S> inputStream,
            @NotNull String input) throws CommandException {

        if (typeResolver == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No type parameter for type '" + type.getTypeName() + "'")
            );
        }
        CommandInputStream<S> copyStream = inputStream.copy();
        //CommandInputStream<S> singleStream = CommandInputStream.ofSingleString(inputStream.currentParameter().orElseThrow(), input);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return typeResolver.resolve(context, copyStream, input);
            } catch (CommandException e) {
                context.imperatConfig()
                        .handleExecutionThrowable(e, context, ParameterCompletableFuture.class, "resolve");
                return null;
            }
        });
    }

    @Override
    public SuggestionResolver<S> getSuggestionResolver() {
        return typeResolver.getSuggestionResolver();
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> context, CommandParameter<S> parameter) {
        return typeResolver.matchesInput(rawPosition, context, parameter);
    }

    @Override
    public OptionalValueSupplier supplyDefaultValue() {
        return typeResolver.supplyDefaultValue();
    }

    @Override
    public boolean isGreedy(CommandParameter<S> parameter) {
        return typeResolver.isGreedy(parameter);
    }

}
