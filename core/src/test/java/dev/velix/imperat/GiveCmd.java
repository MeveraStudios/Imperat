package dev.velix.imperat;

import dev.velix.imperat.annotations.*;
import dev.velix.imperat.components.TestSource;
import dev.velix.imperat.paramtypes.TestPlayer;
import org.jetbrains.annotations.NotNull;

@Command("give")
public class GiveCmd {
    
    @Usage
    public void sword(
            TestSource sender,
            @NotNull @Named("item") @Suggest("lightning") String item,
            @Named("player") @Optional TestPlayer player,
            @Named("amount") @Optional Integer amount
    ) {
        sender.reply("item=" + item + ", target=" + (player == null ? "null" : player.toString()) + ", " + "amount= " + amount) ;
    }

}
