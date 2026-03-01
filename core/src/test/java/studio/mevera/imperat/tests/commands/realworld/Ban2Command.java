package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("ban2")
//@Permission("command.ban")
@Description("Main command for banning players")
public final class Ban2Command {

    @Execute
    public void showUsage(TestSource source) {
        source.reply("/ban <player> [-silent] [duration] [reason...]");
    }

    @Execute
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