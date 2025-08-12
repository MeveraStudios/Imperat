package studio.mevera.imperat.tests.commands.realworld.economy;

import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

import java.math.BigDecimal;

@Command("eco")
public class EconomyCommand {
    
    @Usage
    @Async
    public void defaultUsage(TestSource source) {
    
    }
    
    @SubCommand("add")
    public static class AddCommands {
        
        @Usage
        //@Async
        public void addCurrency(
                final TestSource source,
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
        
        @Usage
        @Async
        public void setCurrency(
                final TestSource source,
                final TestPlayer player,
                final @Optional Currency currency,
                final BigDecimal amount
        ) {
        }
    }
    */
    


}
