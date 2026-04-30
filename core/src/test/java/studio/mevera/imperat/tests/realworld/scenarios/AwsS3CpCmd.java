package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code aws s3 cp <src> <dst> [--recursive] [--acl <acl>] [--storage-class <c>]}.
 *
 * <p>Real-world reference:
 * {@code aws s3 cp ./build s3://my-bucket/dist --recursive --acl public-read}.</p>
 */
@RootCommand("aws")
public final class AwsS3CpCmd {

    public static volatile String LAST_SRC;
    public static volatile String LAST_DST;
    public static volatile Boolean LAST_RECURSIVE;
    public static volatile String LAST_ACL;
    public static volatile String LAST_STORAGE_CLASS;

    @SubCommand("s3")
    public static final class S3 {
        @SubCommand("cp")
        public static final class Cp {
            @Execute
            public void run(
                    TestCommandSource sender,
                    @Named("src") String src,
                    @Named("dst") String dst,
                    @Switch("recursive") Boolean recursive,
                    @Flag("acl") @Default("private") String acl,
                    @Flag("storage-class") @Default("STANDARD") String storageClass
            ) {
                LAST_SRC = src;
                LAST_DST = dst;
                LAST_RECURSIVE = recursive;
                LAST_ACL = acl;
                LAST_STORAGE_CLASS = storageClass;
            }
        }
    }
}
