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
 * Models {@code gradle <tasks...> [--no-daemon] [--offline] [--parallel] [--build-file <f>]}.
 *
 * <p>Real-world reference:
 * {@code gradle build test --no-daemon --offline --parallel}.</p>
 */
@RootCommand("gradle")
public final class GradleCmd {

    public static volatile String LAST_TASKS;
    public static volatile Boolean LAST_NO_DAEMON;
    public static volatile Boolean LAST_OFFLINE;
    public static volatile Boolean LAST_PARALLEL;
    public static volatile String LAST_BUILD_FILE;

    @Execute
    public void run(
            TestCommandSource s,
            @Switch("no-daemon") Boolean noDaemon,
            @Switch("offline") Boolean offline,
            @Switch("parallel") Boolean parallel,
            @Flag({"build-file", "b"}) @Default("build.gradle") String buildFile,
            @Named("tasks") @Greedy String tasks
    ) {
        LAST_TASKS = tasks;
        LAST_NO_DAEMON = noDaemon;
        LAST_OFFLINE = offline;
        LAST_PARALLEL = parallel;
        LAST_BUILD_FILE = buildFile;
    }
}
