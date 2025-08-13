package studio.mevera.imperat.tests.commands.realworld.economy;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.tests.TestSource;

public final class CurrencyParamType extends BaseParameterType<TestSource, Currency> {
    
    
    @Override
    public Currency resolve(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull CommandInputStream<TestSource> inputStream,
            @NotNull String input
    ) throws ImperatException {
        Currency currency = CurrencyManager.getInstance().getCurrencyByName(input.toLowerCase());
        if(currency == null) {
            throw new InvalidCurrencyException(input, context);
        }
        return currency;
    }
    
    @Override
    public boolean matchesInput(String input, CommandParameter<TestSource> parameter) {
        return CurrencyManager.getInstance().getCurrencyByName(input.toLowerCase()) != null;
    }
    
    @Override
    public SuggestionResolver<TestSource> getSuggestionResolver() {
        return (ctx, parameter)->
                CurrencyManager.getInstance().getAllCurrencies()
                .stream()
                .map(Currency::getName)
                .toList();
    }
}
