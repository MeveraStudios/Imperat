package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestSource;

/**
 * /announce [-urgent/-u] [-pin/-p] <message...>
 * <p>
 * A simple command with switches placed directly before a greedy string.
 * Used to test that switches are properly skipped and not consumed
 * as part of the greedy content.
 */
@RootCommand("announce")
public class AnnounceCommand {

    @Execute
    public void announce(
            TestSource source,
            @Switch({"urgent", "u"}) boolean urgent,
            @Switch({"pin", "p"}) boolean pin,
            @Named("message") @Greedy String message
    ) {
        source.reply("urgent=" + urgent);
        source.reply("pin=" + pin);
        source.reply("message=" + message);
    }
}

