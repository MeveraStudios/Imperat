package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Usage;
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