package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code systemctl <action> <unit> [--now] [--quiet]}.
 *
 * <p>Real-world reference:
 * {@code systemctl restart nginx --quiet},
 * {@code systemctl enable docker --now}.</p>
 */
@RootCommand("systemctl")
public final class SystemctlCmd {

    public static volatile String LAST_UNIT;
    public static volatile String LAST_ACTION;
    public static volatile Boolean LAST_NOW;
    public static volatile Boolean LAST_QUIET;

    @SubCommand("restart")
    public static final class Restart {
        @Execute
        public void run(TestCommandSource s, @Named("unit") String unit,
                        @Switch("quiet") Boolean quiet) {
            LAST_ACTION = "restart"; LAST_UNIT = unit; LAST_QUIET = quiet; LAST_NOW = false;
        }
    }

    @SubCommand("enable")
    public static final class Enable {
        @Execute
        public void run(TestCommandSource s, @Named("unit") String unit,
                        @Switch("now") Boolean now,
                        @Switch("quiet") Boolean quiet) {
            LAST_ACTION = "enable"; LAST_UNIT = unit; LAST_NOW = now; LAST_QUIET = quiet;
        }
    }

    @SubCommand("status")
    public static final class Status {
        @Execute
        public void run(TestCommandSource s, @Named("unit") String unit) {
            LAST_ACTION = "status"; LAST_UNIT = unit; LAST_NOW = false; LAST_QUIET = false;
        }
    }
}
