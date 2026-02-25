package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("setrank")
public class SetRankCmd {

    @Execute
    @Permission("voxy.grant")
    public void execute(
            final TestSource actor,
            @Named("target") String data,
            final @Named("rank") String rank,
            final @Named("duration") String duration,
            final @Optional @Named("reason") @Default("Undefined Reason") String reason,
            final @Switch({"extend", "e"}) boolean extend
    ) {
        actor.reply("setting rank for target " + data + " rank=" + rank + " for " + duration + " , reason= " + reason);
        actor.reply("EXTENDED? = " + extend);
    }

}