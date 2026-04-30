package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code ffmpeg -i <input> [-c:v <codec>] [-b:v <bitrate>] [-y] <output>}.
 *
 * <p>Real-world reference:
 * {@code ffmpeg -i input.mp4 -c:v libx264 -b:v 1M -y output.mp4}.</p>
 *
 * <p>Note: real ffmpeg uses {@code -c:v} colon-stream-spec syntax. Imperat
 * flag names must be alphanumeric, so this models them as {@code --vcodec}
 * / {@code --vbitrate} aliases. Test invocations target those names.</p>
 */
@RootCommand("ffmpeg")
public final class FfmpegCmd {

    public static volatile String LAST_INPUT;
    public static volatile String LAST_OUTPUT;
    public static volatile String LAST_VIDEO_CODEC;
    public static volatile String LAST_VIDEO_BITRATE;
    public static volatile Boolean LAST_OVERWRITE;

    @Execute
    public void run(
            TestCommandSource sender,
            @Flag({"input", "i"}) String input,
            @Flag("vcodec") @Default("copy") String vcodec,
            @Flag("vbitrate") @Default("auto") String vbitrate,
            @Switch({"overwrite", "y"}) Boolean overwrite,
            @Named("output") String output
    ) {
        LAST_INPUT = input;
        LAST_OUTPUT = output;
        LAST_VIDEO_CODEC = vcodec;
        LAST_VIDEO_BITRATE = vbitrate;
        LAST_OVERWRITE = overwrite;
    }
}
