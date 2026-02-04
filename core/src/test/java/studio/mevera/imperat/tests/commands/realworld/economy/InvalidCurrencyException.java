package studio.mevera.imperat.tests.commands.realworld.economy;

import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.SelfHandledException;

public final class InvalidCurrencyException extends SelfHandledException {

    private final String input;

    public InvalidCurrencyException(String input) {
        super();
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    @Override
    public <S extends Source> void handle(ImperatConfig<S> imperat, Context<S> context) {
        context.source().reply("Invalid currency '" + input + "'");
    }
}
