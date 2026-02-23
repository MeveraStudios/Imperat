package studio.mevera.imperat.tests.commands.realworld.economy;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestSource;

public final class CurrencyParamType extends ArgumentType<TestSource, Currency> {


    @Override
    public Currency parse(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull Cursor<TestSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        Currency currency = CurrencyManager.getInstance().getCurrencyByName(correspondingInput.toLowerCase());
        if (currency == null) {
            throw new InvalidCurrencyException(correspondingInput);
        }
        return currency;
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<TestSource> context, Argument<TestSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        return CurrencyManager.getInstance().getCurrencyByName(input.toLowerCase()) != null;
    }

    @Override
    public SuggestionProvider<TestSource> getSuggestionProvider() {
        return (ctx, parameter) ->
                       CurrencyManager.getInstance().getAllCurrencies()
                               .stream()
                               .map(Currency::getName)
                               .toList();
    }
}
