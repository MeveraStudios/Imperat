package studio.mevera.imperat.tests.commands.realworld.economy;

import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.SelfHandlingException;

public final class InvalidCurrencyException extends SelfHandlingException {

    private final String input;

    public InvalidCurrencyException(String input) {
        super();
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    @Override
    public <S extends Source> void handle(CommandContext<S> context) {
        context.source().reply("Invalid currency '" + input + "'");
    }
}
