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
 * Models {@code terraform <plan|apply|destroy> [--auto-approve] [--var-file <f>] [--target <addr>]}.
 *
 * <p>Real-world reference:
 * {@code terraform apply --auto-approve --var-file=prod.tfvars --target=module.web}.</p>
 */
@RootCommand("terraform")
public final class TerraformCmd {

    public static volatile String LAST_ACTION;
    public static volatile Boolean LAST_AUTO_APPROVE;
    public static volatile String LAST_VAR_FILE;
    public static volatile String LAST_TARGET;

    @SubCommand("apply")
    public static final class Apply {
        @Execute
        public void run(
                TestCommandSource s,
                @Switch("auto-approve") Boolean autoApprove,
                @Flag("var-file") String varFile,
                @Flag("target") String target
        ) {
            LAST_ACTION = "apply";
            LAST_AUTO_APPROVE = autoApprove;
            LAST_VAR_FILE = varFile;
            LAST_TARGET = target;
        }
    }

    @SubCommand("destroy")
    public static final class Destroy {
        @Execute
        public void run(
                TestCommandSource s,
                @Switch("auto-approve") Boolean autoApprove,
                @Flag("target") String target
        ) {
            LAST_ACTION = "destroy";
            LAST_AUTO_APPROVE = autoApprove;
            LAST_TARGET = target;
            LAST_VAR_FILE = "";
        }
    }

    @SubCommand("plan")
    public static final class Plan {
        @Execute
        public void run(
                TestCommandSource s,
                @Flag("var-file") String varFile
        ) {
            LAST_ACTION = "plan";
            LAST_VAR_FILE = varFile;
            LAST_AUTO_APPROVE = false;
            LAST_TARGET = "";
        }
    }
}
