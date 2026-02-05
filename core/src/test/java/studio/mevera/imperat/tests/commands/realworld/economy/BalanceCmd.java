package studio.mevera.imperat.tests.commands.realworld.economy;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

@Command("bal")
//@Permission("bal.see")
public class BalanceCmd {

    @Execute
    public void def(
            TestSource sender,
            @Default("me") TestPlayer target,
            @Flag({"currency", "c"}) @Default("GOLD") Currency currency
    ) {
        System.out.println("CURRENCY= '" + currency.getName() + "'");
        System.out.println("TARGET= '" + target.toString() + "'");
    }

}