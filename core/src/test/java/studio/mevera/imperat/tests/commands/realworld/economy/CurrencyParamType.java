package studio.mevera.imperat.tests.commands.realworld.economy;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestCommandSource;

public final class CurrencyParamType extends ArgumentType<TestCommandSource, Currency> {


    @Override
    public Currency parse(
            @NotNull ExecutionContext<TestCommandSource> context,
            @NotNull Cursor<TestCommandSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        Currency currency = CurrencyManager.getInstance().getCurrencyByName(correspondingInput.toLowerCase());
        if (currency == null) {
            throw new InvalidCurrencyException(correspondingInput);
        }
        return currency;
    }

    @Override
    public boolean matchesInput(int rawPosition, CommandContext<TestCommandSource> context, Argument<TestCommandSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        return CurrencyManager.getInstance().getCurrencyByName(input.toLowerCase()) != null;
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
