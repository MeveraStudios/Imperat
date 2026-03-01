package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.tests.TestSource;

import java.time.Duration;

@RootCommand("rank")
public class RankCommand {

    @SubCommand(value = "addperm")
    @Description("Adds a permission")
    public void addPerm(
            final TestSource actor,
            @Named("rank") final String rank,
            @Named("permission") String permission,
            @Flag("customDuration") @Default("permanent") Duration customDuration,
            @Switch("force") final boolean force
    ) {
        actor.reply("rank= " + rank);
        actor.reply("perm= " + permission);
        actor.reply("customDuration= " + customDuration.toString());
        actor.reply("forced= " + force);
    }

}