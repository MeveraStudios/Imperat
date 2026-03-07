package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
}