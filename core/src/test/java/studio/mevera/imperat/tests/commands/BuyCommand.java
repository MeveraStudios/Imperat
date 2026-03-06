package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.ExceptionHandler;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Range;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.PermissionDeniedException;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("buy")
public class BuyCommand {

    @Execute
    public void buyItem(TestSource source, String item, @Range(min = 1, max = 50) int quantity) {
        source.reply("You have bought " + quantity + " of " + item + "!");
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public void handlePermissionDenied(PermissionDeniedException ex, CommandContext<TestSource> context) {
        context.source().reply("You don't have permission to buy items.");
    }
}
