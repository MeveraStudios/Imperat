package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.Permission;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
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