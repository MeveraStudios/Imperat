package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code ssh [-i <key>] [-p <port>] [-v] <user@host> [command...]}.
 *
 * <p>Real-world reference:
 * {@code ssh -i ~/.ssh/id_rsa -p 2222 -v admin@example.com uptime}.</p>
 */
@RootCommand("ssh")
public final class SshCmd {

    public static volatile String LAST_DESTINATION;
    public static volatile String LAST_KEY;
    public static volatile String LAST_PORT;
    public static volatile Boolean LAST_VERBOSE;
    public static volatile String LAST_REMOTE_COMMAND;

    @Execute
    public void run(
            TestCommandSource sender,
            @Flag({"identity", "i"}) String identity,
            @Flag({"port", "p"}) @Default("22") String port,
            @Switch({"verbose", "v"}) Boolean verbose,
            @Named("destination") String destination,
            @Named("command") @Optional @Greedy String command
    ) {
        LAST_DESTINATION = destination;
        LAST_KEY = identity;
        LAST_PORT = port;
        LAST_VERBOSE = verbose;
        LAST_REMOTE_COMMAND = command;
    }
}
