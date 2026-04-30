package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code grep [-r] [-n] [-i] [--include <glob>] <pattern> [path]}.
 *
 * <p>Real-world reference:
 * {@code grep -rni --include="*.java" "TODO" src/}.</p>
 */
@RootCommand("grep")
public final class GrepCmd {

    public static volatile String LAST_PATTERN;
    public static volatile String LAST_PATH;
    public static volatile Boolean LAST_RECURSIVE;
    public static volatile Boolean LAST_LINE_NUMBER;
    public static volatile Boolean LAST_IGNORE_CASE;
    public static volatile String LAST_INCLUDE;

    @Execute
    public void run(
            TestCommandSource sender,
            @Switch({"recursive", "r"}) Boolean recursive,
            @Switch({"line-number", "n"}) Boolean lineNumber,
            @Switch({"ignore-case", "i"}) Boolean ignoreCase,
            @Flag("include") String include,
            @Named("pattern") String pattern,
            @Named("path") @Optional @Default(".") String path
    ) {
        LAST_PATTERN = pattern;
        LAST_PATH = path;
        LAST_RECURSIVE = recursive;
        LAST_LINE_NUMBER = lineNumber;
        LAST_IGNORE_CASE = ignoreCase;
        LAST_INCLUDE = include;
    }
}
