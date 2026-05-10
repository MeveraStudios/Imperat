package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestCommandSource;

@DisplayName("Subcommand-Arg Name Collision Regression")
public class SubcommandArgNameCollisionTest extends BaseImperatTest {

    private static volatile Warp captured;

    @BeforeAll
    static void register() {
        IMPERAT.config().registerArgType(Warp.class, new WarpArgumentType());
        IMPERAT.registerCommand(WarpCollisionCommand.class);
    }

    @AfterAll
    static void unregister() {
        IMPERAT.unregisterCommand("clcollision");
    }

    @Test
    @DisplayName("`@SubCommand(\"warp\")` + sibling `@Named(\"warp\")` positional resolves to the positional value, not the CommandImpl")
    void executesPositionalNotSubcommand() {
        captured = null;
        ExecutionResult<TestCommandSource> result = execute("clcollision warp myWarp");
        assertSuccess(result);
        org.junit.jupiter.api.Assertions.assertNotNull(captured, "positional arg was not bound");
        org.junit.jupiter.api.Assertions.assertEquals("myWarp", captured.name);
    }

    public static final class Warp {
        final String name;
        Warp(String name) { this.name = name; }
    }

    public static final class WarpArgumentType extends SimpleArgumentType<TestCommandSource, Warp> {
        @Override
        public Warp parse(@NotNull CommandContext<TestCommandSource> context,
                          @NotNull Argument<TestCommandSource> argument,
                          @NotNull String input) throws CommandException {
            return new Warp(input);
        }
    }

    @RootCommand("clcollision")
    public static final class WarpCollisionCommand {

        @SubCommand("warp")
        public static final class WarpSub {

            @Execute
            public void openGui(TestCommandSource source) {
                source.reply("opened gui");
            }

            @Execute
            public void teleport(TestCommandSource source, @Named("warp") Warp warp) {
                captured = warp;
                source.reply("teleported to " + warp.name);
            }
        }
    }
}
