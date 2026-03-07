package studio.mevera.imperat.tests.commands.realworld.economy;

import studio.mevera.imperat.annotations.types.Async;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

import java.math.BigDecimal;

@RootCommand("eco")
public class EconomyCommand {

    @Execute
    @Async
    public void defaultUsage(TestCommandSource source) {

    }

    @SubCommand("add")
    public static class AddCommands {

        @Execute
        //@Async
        public void addCurrency(
                final TestCommandSource source,
                final TestPlayer player,
                final @Default("gold") Currency currency,
                final BigDecimal amount
        ) {
            System.out.println("Currency= " + currency.getName() +
                                       ", amount= " + amount.toPlainString());
        }
    }
    
    /*    @SubCommand("set")
    public static class SetCommands {
        
        @Execute
        @Async
        public void setCurrency(
                final TestCommandSource source,
                final TestPlayer player,
                final @Optional Currency currency,
                final BigDecimal amount
        ) {
        }
    }
    */


}
