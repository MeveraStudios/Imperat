package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.ExceptionHandler;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Range;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.tests.TestCommandSource;

@RootCommand("buy")
public class BuyCommand {

    public static volatile String lastHandledExceptionMessage = null;

    @Execute
    public void buyItem(TestCommandSource source, String item, @Range(min = 1, max = 50) int quantity) {
        source.reply("You have bought " + quantity + " of " + item + "!");
    }

    @ExceptionHandler(ResponseException.class)
    public void handleResponseException(ResponseException exception, CommandContext<TestCommandSource> context) {
        lastHandledExceptionMessage = "BUY_HANDLER: " + exception.getResponseKey().getKey();
        context.source().reply("Buy command error: invalid quantity specified");
    }

}
