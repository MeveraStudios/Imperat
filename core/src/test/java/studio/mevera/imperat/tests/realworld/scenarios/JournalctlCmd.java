package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code journalctl [-u <unit>] [--since <ts>] [-n <lines>] [-f]}.
 *
 * <p>Real-world reference:
 * {@code journalctl -u nginx --since yesterday -n 100 -f}.</p>
 */
@RootCommand("journalctl")
public final class JournalctlCmd {

    public static volatile String LAST_UNIT;
    public static volatile String LAST_SINCE;
    public static volatile String LAST_LINES;
    public static volatile Boolean LAST_FOLLOW;

    @Execute
    public void run(
            TestCommandSource s,
            @Flag({"unit", "u"}) String unit,
            @Flag("since") String since,
            @Flag({"lines", "n"}) @Default("10") String lines,
            @Switch({"follow", "f"}) Boolean follow
    ) {
        LAST_UNIT = unit;
        LAST_SINCE = since;
        LAST_LINES = lines;
        LAST_FOLLOW = follow;
    }
}
