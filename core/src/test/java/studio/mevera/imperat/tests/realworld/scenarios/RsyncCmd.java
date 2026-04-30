package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code rsync [-a] [-v] [-z] [--delete] [--exclude <pat>] <src> <dst>}.
 *
 * <p>Real-world reference:
 * {@code rsync -a -v -z --exclude=.git src/ user@host:/backup/}.</p>
 */
@RootCommand("rsync")
public final class RsyncCmd {

    public static volatile String LAST_SRC;
    public static volatile String LAST_DST;
    public static volatile Boolean LAST_ARCHIVE;
    public static volatile Boolean LAST_VERBOSE;
    public static volatile Boolean LAST_COMPRESS;
    public static volatile Boolean LAST_DELETE;
    public static volatile String LAST_EXCLUDE;

    @Execute
    public void run(
            TestCommandSource s,
            @Switch({"archive", "a"}) Boolean archive,
            @Switch({"verbose", "v"}) Boolean verbose,
            @Switch({"compress", "z"}) Boolean compress,
            @Switch("delete") Boolean delete,
            @Flag("exclude") String exclude,
            @Named("src") String src,
            @Named("dst") String dst
    ) {
        LAST_SRC = src;
        LAST_DST = dst;
        LAST_ARCHIVE = archive;
        LAST_VERBOSE = verbose;
        LAST_COMPRESS = compress;
        LAST_DELETE = delete;
        LAST_EXCLUDE = exclude;
    }
}
