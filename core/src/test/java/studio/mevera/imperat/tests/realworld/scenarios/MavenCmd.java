package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code mvn <goals...> [-P <profile>] [--offline] [-T <threads>]}.
 *
 * <p>Real-world reference:
 * {@code mvn clean install -P prod -T 4 --offline}.</p>
 */
@RootCommand("mvn")
public final class MavenCmd {

    public static volatile String LAST_GOALS;
    public static volatile String LAST_PROFILE;
    public static volatile Boolean LAST_OFFLINE;
    public static volatile String LAST_THREADS;

    @Execute
    public void run(
            TestCommandSource s,
            @Flag({"profile", "P"}) String profile,
            @Switch("offline") Boolean offline,
            @Flag({"threads", "T"}) @Default("1") String threads,
            @Named("goals") @Greedy String goals
    ) {
        LAST_GOALS = goals;
        LAST_PROFILE = profile;
        LAST_OFFLINE = offline;
        LAST_THREADS = threads;
    }
}
