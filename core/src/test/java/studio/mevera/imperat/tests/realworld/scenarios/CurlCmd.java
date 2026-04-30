package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code curl [-X <method>] [-H <header>] [-d <data>] [-o <file>] [-L] <url>}.
 *
 * <p>Real-world reference:
 * {@code curl -X POST -H "Content-Type: application/json" -d "{}" -o out.json https://api.example.com}.</p>
 *
 * <p>Multi-occurrence headers (-H -H -H) not modelled — Imperat takes a
 * single value per flag occurrence.</p>
 */
@RootCommand("curl")
public final class CurlCmd {

    public static volatile String LAST_URL;
    public static volatile String LAST_METHOD;
    public static volatile String LAST_HEADER;
    public static volatile String LAST_DATA;
    public static volatile String LAST_OUTPUT;
    public static volatile Boolean LAST_FOLLOW;

    @Execute
    public void run(
            TestCommandSource sender,
            @Flag({"request", "X"}) @Default("GET") String method,
            @Flag({"header", "H"}) String header,
            @Flag({"data", "d"}) String data,
            @Flag({"output", "o"}) String output,
            @Switch({"location", "L"}) Boolean follow,
            @Named("url") String url
    ) {
        LAST_URL = url;
        LAST_METHOD = method;
        LAST_HEADER = header;
        LAST_DATA = data;
        LAST_OUTPUT = output;
        LAST_FOLLOW = follow;
    }
}
