package studio.mevera.imperat.tests.commands.realworld.economy;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

@RootCommand("bal")
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