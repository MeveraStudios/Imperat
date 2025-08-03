package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.tests.TestSource;

import java.time.Duration;

@Command("rank")
public class RankCommand {

    @SubCommand("addperm")
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