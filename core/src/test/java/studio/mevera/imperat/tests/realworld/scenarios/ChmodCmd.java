package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code chmod [-R] [-v] <mode> <path>}.
 *
 * <p>Real-world reference: {@code chmod -R 755 /var/www}.</p>
 */
@RootCommand("chmod")
public final class ChmodCmd {

    public static volatile String LAST_MODE;
    public static volatile String LAST_PATH;
    public static volatile Boolean LAST_RECURSIVE;
    public static volatile Boolean LAST_VERBOSE;

    @Execute
    public void run(
            TestCommandSource s,
            @Switch({"recursive", "R"}) Boolean recursive,
            @Switch({"verbose", "v"}) Boolean verbose,
            @Named("mode") String mode,
            @Named("path") String path
    ) {
        LAST_MODE = mode;
        LAST_PATH = path;
        LAST_RECURSIVE = recursive;
        LAST_VERBOSE = verbose;
    }
}
