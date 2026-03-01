package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Range;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("buy")
public class BuyCommand {

    @Execute
    public void buyItem(TestSource source, String item, @Range(min = 1, max = 50) int quantity) {
        source.reply("You have bought " + quantity + " of " + item + "!");
    }
}
