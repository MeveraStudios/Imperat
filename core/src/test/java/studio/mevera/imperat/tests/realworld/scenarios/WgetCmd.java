package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code wget [-O <out>] [-q] [--user-agent <ua>] [--no-check-certificate] <url>}.
 *
 * <p>Real-world reference:
 * {@code wget -O page.html --user-agent=Mozilla --no-check-certificate https://example.com}.</p>
 */
@RootCommand("wget")
public final class WgetCmd {

    public static volatile String LAST_URL;
    public static volatile String LAST_OUTPUT;
    public static volatile Boolean LAST_QUIET;
    public static volatile String LAST_USER_AGENT;
    public static volatile Boolean LAST_NO_CHECK_CERT;

    @Execute
    public void run(
            TestCommandSource s,
            @Flag({"output-document", "O"}) String output,
            @Switch({"quiet", "q"}) Boolean quiet,
            @Flag("user-agent") String userAgent,
            @Switch("no-check-certificate") Boolean noCheckCert,
            @Named("url") String url
    ) {
        LAST_URL = url;
        LAST_OUTPUT = output;
        LAST_QUIET = quiet;
        LAST_USER_AGENT = userAgent;
        LAST_NO_CHECK_CERT = noCheckCert;
    }
}
