package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code find <path> [--name <pat>] [--type <t>] [--mtime <d>] [--delete]}.
 *
 * <p>Real-world reference:
 * {@code find /var/log --name "*.log" --type f --mtime +7 --delete}.</p>
 *
 * <p>Note: real find uses single-dash long options ({@code -name},
 * {@code -type}, {@code -delete}) which conflicts with our short-form
 * combine convention; the Imperat-mapped form uses GNU-style long
 * prefixes for clarity.</p>
 */
@RootCommand("find")
public final class FindCmd {

    public static volatile String LAST_PATH;
    public static volatile String LAST_NAME;
    public static volatile String LAST_TYPE;
    public static volatile String LAST_MTIME;
    public static volatile Boolean LAST_DELETE;

    @Execute
    public void run(
            TestCommandSource s,
            @Named("path") String path,
            @Flag("name") String name,
            @Flag("type") String type,
            @Flag("mtime") String mtime,
            @Switch("delete") Boolean delete
    ) {
        LAST_PATH = path;
        LAST_NAME = name;
        LAST_TYPE = type;
        LAST_MTIME = mtime;
        LAST_DELETE = delete;
    }
}
