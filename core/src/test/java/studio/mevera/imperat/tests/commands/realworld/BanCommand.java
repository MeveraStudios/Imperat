package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.Range;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("ban")
@Description("Main command for banning players")
public final class BanCommand {

    @Execute
    public void showUsage(TestSource source) {
        source.reply("/ban <player> [-silent] [duration] [reason...]");
    }

    @Execute
    public void ban(
            TestSource source,
            @Named("target") String player,
            @Switch({"silent", "s"}) boolean silent,
            @Switch("ip") boolean ip,
            @Named("duration") @Default("permanent") @Nullable String duration,
            @Named("reason") @Default("Breaking server laws") @Optional @Greedy String reason
    ) {
        //TODO actual ban logic
        String durationFormat = duration == null ? "FOREVER" : "for " + duration;
        String msg = "Banning " + player + " " + durationFormat + " due to '" + reason + "'";
        System.out.println("IP= " + ip);
        System.out.println("SILENT= " + silent);


        if (!silent) {
            source.reply("NOT SILENT= " + msg);
        } else {
            source.reply("SILENT= " + msg);
        }
    }

    @RootCommand("printnum")
    public void printNum(TestSource source, @Named("num") @Range(min = 1.0) int num) {
        source.reply("NUM= " + num);
    }

}