package studio.mevera.imperat.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import studio.mevera.imperat.CommandLineImperat;
import studio.mevera.imperat.ConsoleCommandSource;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.ExternalSubCommand;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.ExecutionResult;

import java.io.InputStream;

/**
 * Minimal reproduction for Imperat upstream issue:
 * routes declared on external helper classes via {@link ExternalSubCommand}
 * must be attached to the registered {@link Command} tree after
 * {@link studio.mevera.imperat.Imperat#registerCommand(Object)}.
 */
final class ImperatExternalSubcommandReproductionTest {

    private static CommandLineImperat newImperat() {
        CommandLineImperat imperat = CommandLineImperat.builder(InputStream.nullInputStream()).build();
        imperat.registerCommand(new DemoRootCommand());
        return imperat;
    }

    @Test
    void externalSubCommandShouldRegisterChildOnRootCommand() {
        CommandLineImperat imperat = newImperat();

        Command<ConsoleCommandSource> demo = imperat.getCommand("demo");
        assertNotNull(demo, "root command demo should exist");

        assertFalse(
                demo.getSubCommands().isEmpty(),
                "Expected @ExternalSubCommand helper (DemoExternal) to contribute a 'child' subcommand on 'demo'.");

        assertNotNull(
                imperat.getSubCommand("demo", "child"),
                "Expected getSubCommand(\"demo\", \"child\") to resolve the external @SubCommand route.");
    }

    @Test
    void externalSubCommandExecutionShouldSucceed() {
        CommandLineImperat imperat = newImperat();

        ExecutionResult<ConsoleCommandSource> result =
                imperat.execute(imperat.createDummySender(), "demo", new String[]{"child"});

        assertFalse(
                result.hasFailed(),
                "Executing 'demo child' should succeed once external routes are wired. "
                        + "Failure means the parser never reaches DemoExternal#child.");
    }

    @RootCommand("demo")
    @ExternalSubCommand(DemoExternal.class)
    public static final class DemoRootCommand {

        @Execute
        public void root(ConsoleCommandSource source) {
        }
    }

    @SubCommand("child")
    public static final class DemoExternal {

        @Execute
        public void child(ConsoleCommandSource source) {
        }
    }
}
