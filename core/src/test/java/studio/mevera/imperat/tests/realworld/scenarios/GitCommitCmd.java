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
 * Models {@code git commit -m "<msg>" [--amend] [--no-verify] [-a]}.
 *
 * <p>Real-world reference: {@code git commit -m "fix: bug" --amend --no-verify}.</p>
 */
@RootCommand("git")
public final class GitCommitCmd {

    public static volatile String LAST_MESSAGE;
    public static volatile Boolean LAST_AMEND;
    public static volatile Boolean LAST_NO_VERIFY;
    public static volatile Boolean LAST_ALL;

    @SubCommand("commit")
    public static final class Commit {
        @Execute
        public void run(
                TestCommandSource sender,
                @Flag({"message", "m"}) String message,
                @Switch("amend") Boolean amend,
                @Switch("no-verify") Boolean noVerify,
                @Switch({"all", "a"}) Boolean all
        ) {
            LAST_MESSAGE = message;
            LAST_AMEND = amend;
            LAST_NO_VERIFY = noVerify;
            LAST_ALL = all;
        }
    }
}
