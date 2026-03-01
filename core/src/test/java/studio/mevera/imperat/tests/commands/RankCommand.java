package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Switch;
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