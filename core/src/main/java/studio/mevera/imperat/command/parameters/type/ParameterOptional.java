package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Optional;

public final class ParameterOptional<S extends Source, T> extends BaseParameterType<S, Optional<T>> {

    private final ParameterType<S, T> typeResolver;

    public ParameterOptional(TypeWrap<Optional<T>> typeWrap, ParameterType<S, T> typeResolver) {
        super(typeWrap.getType());
        this.typeResolver = typeResolver;
    }

    @Override
    public @NotNull Optional<T> resolve(
            @NotNull ExecutionContext<S> context,
            @NotNull CommandInputStream<S> inputStream,
            @NotNull String input
    ) throws ImperatException {
        return Optional.ofNullable(
                typeResolver.resolve(context,inputStream, input)
        );
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
}
