package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

@Command("give")
public class GiveCmd {
    
    @Usage
    public void sword(
            TestSource sender,
            @NotNull @Named("item") @Suggest("lightning") String item,
            @Named("player") @Optional TestPlayer player,
            @Named("amount") @Default("1") @Suggest({"1", "2", "3"}) Integer amount
    ) {
        sender.reply("item=" + item + ", target=" + (player == null ? "null" : player.toString()) + ", " + "amount= " + amount) ;
    }

}