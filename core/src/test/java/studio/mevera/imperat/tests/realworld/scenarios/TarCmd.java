package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code tar [-c] [-x] [-z] [-v] [-f <archive>] <files...>}.
 *
 * <p>Real-world reference:
 * {@code tar -czvf backup.tar.gz src/}.</p>
 *
 * <p>Note: real tar accepts smashed-form {@code -czvf <archive>} as a
 * single token. Imperat's alias-combine handles {@code -czv} but the
 * archive flag {@code -f} requires its value to follow as the next
 * token; tests use the canonical {@code --create --gzip --verbose
 * --file backup.tar.gz src/} form to exercise the equivalence.</p>
 */
@RootCommand("tar")
public final class TarCmd {

    public static volatile Boolean LAST_CREATE;
    public static volatile Boolean LAST_EXTRACT;
    public static volatile Boolean LAST_GZIP;
    public static volatile Boolean LAST_VERBOSE;
    public static volatile String LAST_ARCHIVE;
    public static volatile String LAST_FILES;

    @Execute
    public void run(
            TestCommandSource sender,
            @Switch({"create", "c"}) Boolean create,
            @Switch({"extract", "x"}) Boolean extract,
            @Switch({"gzip", "z"}) Boolean gzip,
            @Switch({"verbose", "v"}) Boolean verbose,
            @Flag({"file", "f"}) String archive,
            @Named("files") @Greedy String files
    ) {
        LAST_CREATE = create;
        LAST_EXTRACT = extract;
        LAST_GZIP = gzip;
        LAST_VERBOSE = verbose;
        LAST_ARCHIVE = archive;
        LAST_FILES = files;
    }
}
