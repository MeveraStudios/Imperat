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
 * Models {@code docker run [-d] [--rm] [--name <n>] [-p <port>] <image>}.
 *
 * <p>Real-world reference:
 * {@code docker run -d --rm --name web -p 8080:80 nginx:latest}.</p>
 */
@RootCommand("docker")
public final class DockerRunCmd {

    public static volatile String LAST_IMAGE;
    public static volatile Boolean LAST_DETACH;
    public static volatile Boolean LAST_RM;
    public static volatile String LAST_NAME;
    public static volatile String LAST_PORT;

    @SubCommand("run")
    public static final class Run {
        @Execute
        public void run(
                TestCommandSource sender,
                @Switch({"detach", "d"}) Boolean detach,
                @Switch("rm") Boolean rm,
                @Flag("name") String name,
                @Flag({"publish", "p"}) String port,
                @Named("image") String image
        ) {
            LAST_IMAGE = image;
            LAST_DETACH = detach;
            LAST_RM = rm;
            LAST_NAME = name;
            LAST_PORT = port;
        }
    }
}
