package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Suggest;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;

@DisplayName("Enhanced Flag Tests")
class EnhancedFlagArgumentTest extends EnhancedBaseImperatTest {

    @Nested
    @DisplayName("Switch Flag Scenarios")
    class SwitchFlagArgumentScenarios {

        @Test
        @DisplayName("Should handle basic switch flags")
        void testBasicSwitchFlags() {
            ExecutionResult<TestCommandSource> result = execute("ban mqzen");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("target", "mqzen")
                    .hasSwitchDisabled("silent")
                    .hasSwitchDisabled("ip")
                    .hasArgument("reason", "Breaking server laws");
        }

        @Test
        @DisplayName("Should handle activated switches")
        void testActivatedSwitches() {
            ExecutionResult<TestCommandSource> result = execute("ban mqzen -s -ip");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("target", "mqzen")
                    .hasSwitchEnabled("silent")
                    .hasSwitchEnabled("ip");
        }

        @Test
        @DisplayName("Should handle switch order independence")
        void testSwitchOrderIndependence() {
            ExecutionResult<TestCommandSource> firstOrder = execute("ban player1 -s -ip");
            ExecutionResult<TestCommandSource> secondOrder = execute("ban player1 -ip");

            assertThat(firstOrder)
                    .isSuccessful()
                    .hasSwitchEnabled("silent")
                    .hasSwitchEnabled("ip");

            assertThat(secondOrder)
                    .isSuccessful()
                    .hasSwitchDisabled("silent")
                    .hasSwitchEnabled("ip");
        }
    }

    @RootCommand("example")
    public static final class FirstFlagCompletionCommand {

        @Execute
        public void exec(
                TestCommandSource source,
                @Flag({"scenario", "sc"})
                @Suggest({"kindergarten", "castle", "sandstorm", "tsunami"})
                String scenario
        ) {
        }
    }

    @Nested
    @DisplayName("Mixed Flag and Argument Scenarios")
    class MixedFlagArgumentArgumentScenarios {

        @Test
        @DisplayName("Should handle complex flag and argument mixing")
        void testComplexFlagArgumentMixing() {
            ExecutionResult<TestCommandSource> result = execute("ban troublemaker -s -ip 30d Continuous griefing and harassment");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("target", "troublemaker")
                    .hasSwitchEnabled("silent")
                    .hasSwitchEnabled("ip")
                    .hasArgument("duration", "30d")
                    .hasArgument("reason", "Continuous griefing and harassment");
        }
        
        /*@Test
        @DisplayName("Should handle rank command with duration and force flags")
        void testRankCommandWithFlags() {
            ExecutionResult<TestCommandSource> result = execute("rank addperm admin server.op -customDuration 1h -force");
            
            assertThat(result)
                .isSuccessful()
                .hasArgument("rank", "admin")
                .hasArgument("permission", "server.op")
                .hasSwitchEnabled("force")
                .hasFlagValue("customDuration", Duration.ofHours(1));
        }*/
    }

    @RootCommand("multiflag")
    public static final class LeafFlagContinuationCommand {

        @Execute
        public void exec(
                TestCommandSource source,
                String target,
                @Flag({"scenario", "sc"})
                @Suggest({"kindergarten", "castle", "sandstorm", "tsunami"})
                String scenario,
                @Flag({"mode", "m"})
                @Suggest({"safe", "hard"})
                String mode
        ) {
        }
    }

    @RootCommand("example2")
    public static final class SubcommandFirstFlagCompletionCommand {

        @SubCommand("play")
        public void exec(
                TestCommandSource source,
                @Flag({"scenario", "sc"})
                @Suggest({"kindergarten", "castle", "sandstorm", "tsunami"})
                String scenario
        ) {
        }
    }

    @Nested
    @DisplayName("Value Flag Scenarios")
    class ValueFlagArgumentScenarios {

        @Test
        @DisplayName("Should handle value flags with quotes")
        void testValueFlagsWithQuotes() {
            ExecutionResult<TestCommandSource> result = execute("git commit -m \"Initial commit with spaces\"");

            assertThat(result)
                    .isSuccessful()
                    .hasFlagValue("message", "Initial commit with spaces");
        }

        @Test
        @DisplayName("Should handle value flags without quotes")
        void testValueFlagsWithoutQuotes() {
            ExecutionResult<TestCommandSource> result = execute("git commit -m simple_commit");

            assertThat(result)
                    .isSuccessful()
                    .hasFlagValue("message", "simple_commit");
        }

        @Test
        @DisplayName("Should handle time flags in ban2 command")
        void testTimeFlagsInBan2() {
            ExecutionResult<TestCommandSource> result = execute("ban2 player -t 7d Griefing spawn");

            assertThat(result)
                    .isSuccessful()
                    .hasArgument("target", "player")
                    .hasFlagValue("time", "7d")
                    .hasArgument("reason", "Griefing spawn");
        }

        @Test
        @DisplayName("Should suggest cached flag names without subtree scanning")
        void testFlagNameTabCompletion() {
            var suggestions = tabComplete("git commit ");

            Assertions.assertThat(suggestions)
                    .containsExactlyInAnyOrder("-message", "-m");
        }

        @Test
        @DisplayName("Should not suggest a flag again after it was already entered")
        void testUsedFlagIsNotSuggestedAgain() {
            var suggestions = tabComplete("git commit -m initial ");

            Assertions.assertThat(suggestions).isEmpty();
        }

        @Test
        @DisplayName("Should suggest first-position value flag names at root completion")
        void testFirstPositionValueFlagNameCompletion() {
            var suggestions = tabComplete(FirstFlagCompletionCommand.class, cfg -> {
            }, "example ");

            Assertions.assertThat(suggestions)
                    .contains("-scenario", "-sc");
        }

        @Test
        @DisplayName("Should suggest first-position value flag values after alias input")
        void testFirstPositionValueFlagValueCompletionViaAlias() {
            var suggestions = tabComplete(FirstFlagCompletionCommand.class, cfg -> {
            }, "example -sc ");

            Assertions.assertThat(suggestions)
                    .containsExactlyInAnyOrder("kindergarten", "castle", "sandstorm", "tsunami");
        }

        @Test
        @DisplayName("Should suggest first-position value flag values after full name input")
        void testFirstPositionValueFlagValueCompletionViaName() {
            var suggestions = tabComplete(FirstFlagCompletionCommand.class, cfg -> {
            }, "example -scenario ");

            Assertions.assertThat(suggestions)
                    .containsExactlyInAnyOrder("kindergarten", "castle", "sandstorm", "tsunami");
        }

        @Test
        @DisplayName("Should suggest first-position value flag names after a bare dash prefix")
        void testFirstPositionValueFlagNameCompletionViaBareDash() {
            var suggestions = tabComplete(FirstFlagCompletionCommand.class, cfg -> {
            }, "example -");

            Assertions.assertThat(suggestions)
                    .contains("-scenario", "-sc");
        }

        @Test
        @DisplayName("Should execute a root usage made only of a value flag")
        void testFlagOnlyRootUsageExecution() {
            ExecutionResult<TestCommandSource> result = execute(
                    FirstFlagCompletionCommand.class,
                    cfg -> {
                    },
                    "example -sc castle"
            );

            assertThat(result)
                    .isSuccessful()
                    .hasFlagValue("scenario", "castle");
        }

        @Test
        @DisplayName("Should continue suggesting remaining flags after consuming one at a leaf")
        void testRemainingLeafFlagSuggestionsAfterUsedValueFlag() {
            var suggestions = tabComplete(LeafFlagContinuationCommand.class, cfg -> {
            }, "multiflag player -sc castle ");

            Assertions.assertThat(suggestions)
                    .contains("-mode", "-m")
                    .doesNotContain("-scenario", "-sc");
        }

        @Test
        @DisplayName("Should suggest first-position subcommand flag names")
        void testSubcommandFirstPositionValueFlagNameCompletion() {
            var suggestions = tabComplete(SubcommandFirstFlagCompletionCommand.class, cfg -> {
            }, "example2 play ");

            Assertions.assertThat(suggestions)
                    .contains("-scenario", "-sc");
        }

        @Test
        @DisplayName("Should suggest first-position subcommand flag values after alias input")
        void testSubcommandFirstPositionValueFlagValueCompletionViaAlias() {
            var suggestions = tabComplete(SubcommandFirstFlagCompletionCommand.class, cfg -> {
            }, "example2 play -sc ");

            Assertions.assertThat(suggestions)
                    .containsExactlyInAnyOrder("kindergarten", "castle", "sandstorm", "tsunami");
        }

        @Test
        @DisplayName("Should execute a subcommand usage made only of a value flag")
        void testFlagOnlySubcommandUsageExecution() {
            ExecutionResult<TestCommandSource> result = execute(
                    SubcommandFirstFlagCompletionCommand.class,
                    cfg -> {
                    },
                    "example2 play -sc castle"
            );

            assertThat(result)
                    .isSuccessful()
                    .hasFlagValue("scenario", "castle");
        }
    }
}
