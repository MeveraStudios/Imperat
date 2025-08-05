package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.tests.TestSource;

import java.time.Duration;

@Command("ban")
@Description("Main command for banning players")
public final class BanCommand {

    @Usage
    public void showUsage(TestSource source) {
        source.reply("/ban <player> [-silent] [duration] [reason...]");
    }

    @Usage
    public void ban(
            TestSource source,
            @Named("target") String player,
            @Switch({"silent", "s"}) boolean silent,
            @Switch("ip") boolean ip,
            @Named("duration") @Default("permanent") @Nullable Duration duration,
            @Named("reason") @Default("Breaking server laws") @Optional @Greedy String reason
    ) {
        //TODO actual ban logic
        String durationFormat = duration == null ? "FOREVER" : "for " + duration;
        String msg = "Banning " + player + " " + durationFormat + " due to '" + reason + "'";
        System.out.println("IP= " + ip);
        System.out.println("SILENT= " + silent);


        if (!silent)
            source.reply("NOT SILENT= " + msg);
        else
            source.reply("SILENT= " + msg);
    }

    @Command("printnum")
    public void printNum(TestSource source, @Named("num") @Range(min = 1.0) int num) {
        source.reply("NUM= " + num);
    }

}