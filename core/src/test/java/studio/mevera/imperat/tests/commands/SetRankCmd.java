package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.tests.TestSource;

@Command("setrank")
public class SetRankCmd {
    
    @Usage
    @Permission("voxy.grant")
    public void execute(
            final TestSource actor,
            @Named("target") String data,
            final @Named("rank") String rank,
            final @Named("duration") String duration,
            final @Optional @Named("reason") @Default("Undefined Reason") String reason,
            final @Switch({"extend", "e"}) boolean extend
    ) {
        actor.reply("setting rank for target " + data + " rank=" +rank + " for " + duration + " , reason= " + reason);
        actor.reply("EXTENDED? = " + extend);
    }

}