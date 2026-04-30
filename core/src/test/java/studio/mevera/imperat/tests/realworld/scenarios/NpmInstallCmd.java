package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code npm install <pkg> [--save-dev] [--global] [--no-save]}.
 *
 * <p>Real-world reference:
 * {@code npm install lodash --save-dev}.</p>
 */
@RootCommand("npm")
public final class NpmInstallCmd {

    public static volatile String LAST_PACKAGE;
    public static volatile Boolean LAST_SAVE_DEV;
    public static volatile Boolean LAST_GLOBAL;
    public static volatile Boolean LAST_NO_SAVE;

    @SubCommand("install")
    public static final class Install {
        @Execute
        public void run(
                TestCommandSource sender,
                @Named("package") String pkg,
                @Switch({"save-dev", "D"}) Boolean saveDev,
                @Switch({"global", "g"}) Boolean global,
                @Switch("no-save") Boolean noSave
        ) {
            LAST_PACKAGE = pkg;
            LAST_SAVE_DEV = saveDev;
            LAST_GLOBAL = global;
            LAST_NO_SAVE = noSave;
        }
    }
}
