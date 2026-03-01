package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Secret;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Suggest;
import studio.mevera.imperat.tests.TestSource;

/**
 * Test command with secret subcommands for verifying that secret commands
 * are hidden from tab completion suggestions.
 */
@RootCommand("secrettest")
public class SecretCommand {

    @Execute
    public void defaultUsage(TestSource source) {
        source.reply("secrettest default");
    }

    /**
     * A normal (non-secret) subcommand — should appear in suggestions.
     */
    @SubCommand("visible")
    public static class VisibleSub {

        @Execute
        public void run(TestSource source) {
            source.reply("visible executed");
        }

        @SubCommand("child")
        public static class VisibleChild {

            @Execute
            public void run(TestSource source, @Named("arg") @Suggest({"alpha", "beta"}) String arg) {
                source.reply("visible child arg=" + arg);
            }
        }
    }

    /**
     * A secret subcommand — should NOT appear in suggestions.
     */
    @Secret
    @SubCommand("hidden")
    public static class HiddenSub {

        @Execute
        public void run(TestSource source) {
            source.reply("hidden executed");
        }

        @SubCommand("deep")
        public static class HiddenDeep {

            @Execute
            public void run(TestSource source, @Named("val") @Suggest({"x", "y", "z"}) String val) {
                source.reply("hidden deep val=" + val);
            }
        }
    }

    /**
     * Another normal subcommand — should appear in suggestions.
     */
    @SubCommand("public")
    public static class PublicSub {

        @Execute
        public void run(TestSource source, @Named("name") @Suggest({"alice", "bob"}) String name) {
            source.reply("public name=" + name);
        }
    }
}

