package studio.mevera.imperat.tests.commands.realworld.economy;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestCommandSource;

public final class CurrencyParamType extends ArgumentType<TestCommandSource, Currency> {


    @Override
    public Currency parse(
            @NotNull CommandContext<TestCommandSource> context,
            @NotNull Argument<TestCommandSource> argument, @NotNull String correspondingInput
    ) throws CommandException {
        Currency currency = CurrencyManager.getInstance().getCurrencyByName(correspondingInput.toLowerCase());
        if (currency == null) {
            throw new CommandException("Invalid currency: " + correspondingInput);
        }
        return currency;
    }

    @Override
    public SuggestionProvider<TestCommandSource> getSuggestionProvider() {
        return (ctx, parameter) ->
                       CurrencyManager.getInstance().getAllCurrencies()
                               .stream()
                               .map(Currency::getName)
                               .toList();
    }
}
