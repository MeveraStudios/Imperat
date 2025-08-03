package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.tests.TestSource;

@Command("ban2")
@Permission("command.ban")
@Description("Main command for banning players")
public final class Ban2Command {
    
    @Usage
    public void showUsage(TestSource source) {
        source.reply("/ban <player> [-silent] [duration] [reason...]");
    }
    
    @Usage
    public void ban(
            TestSource source,
            @Named("target") String player,
            @Flag({"time", "t"}) @Nullable String time,
            @Named("reason") @Default("Breaking server laws") @Optional @Greedy String reason
    ) {
        //TODO actual ban logic
        String durationFormat = time == null ? "FOREVER" : "for " + time;
        String msg = "Banning " + player + " " + durationFormat + " due to '" + reason + "'";
        System.out.println(msg);
    }
}