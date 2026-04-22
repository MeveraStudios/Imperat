package studio.mevera.imperat.tests.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.InheritedArg;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.Group;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.GroupArgument;
import studio.mevera.imperat.tests.enhanced.EnhancedBaseImperatTest;
import studio.mevera.imperat.tests.parameters.TestPlayerParamType;

@DisplayName("Custom type closest-usage regression")
class CustomTypeClosestUsageRegressionTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Invalid custom type should still report the custom branch as closest usage")
    void invalidCustomType_reportsCustomBranch() {
        ExecutionResult<TestCommandSource> result = execute(
                ReportTraversalRegressionCommand.class,
                cfg -> {
                    cfg.registerArgType(TestPlayer.class, new TestPlayerParamType());
                    cfg.registerArgType(Group.class, new GroupArgument());
                },
                "reportreg mqzen unknown close"
        );

        assertThat(result).hasFailedWith(InvalidSyntaxException.class);

        InvalidSyntaxException exception = assertInstanceOf(InvalidSyntaxException.class, result.getError());
        assertNotNull(exception.getClosestUsage());
        assertEquals("<player> <category> close", exception.getClosestUsage().formatted());
    }

    @Test
    @DisplayName("Lower-priority generic branch should still execute when it fully matches")
    void genericBranch_stillExecutesWhenValid() {
        ExecutionResult<TestCommandSource> result = execute(
                ReportTraversalRegressionCommand.class,
                cfg -> {
                    cfg.registerArgType(TestPlayer.class, new TestPlayerParamType());
                    cfg.registerArgType(Group.class, new GroupArgument());
                },
                "reportreg mqzen note confirm"
        );

        assertThat(result)
                .isSuccessful()
                .hasArgument("message", "note");
    }

    @RootCommand("reportreg")
    public static final class ReportTraversalRegressionCommand {

        @Execute
        public void categoryBase(
                TestCommandSource source,
                @Named("player") TestPlayer player,
                @Named("category") Group category
        ) {
            source.reply(player + " -> " + category.name());
        }

        @Execute
        public void messageBase(
                TestCommandSource source,
                @Named("player") TestPlayer player,
                @Named("message") String message
        ) {
            source.reply(player + " -> " + message);
        }

        @SubCommand(value = "close", attachTo = "<category>")
        public void closeCategory(
                TestCommandSource source,
                @InheritedArg @Named("player") TestPlayer player,
                @InheritedArg @Named("category") Group category
        ) {
            source.reply("closed " + player + " for " + category.name());
        }

        @SubCommand(value = "confirm", attachTo = "<message>")
        public void confirmMessage(
                TestCommandSource source,
                @InheritedArg @Named("player") TestPlayer player,
                @InheritedArg @Named("message") String message
        ) {
            source.reply("confirmed " + player + " with " + message);
        }
    }
}
