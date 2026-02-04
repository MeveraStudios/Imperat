package studio.mevera.imperat.tests.flags;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

@DisplayName("Flags and Switches Tests")
public class FlagsAndSwitchesTest extends BaseImperatTest {

    @Test
    @DisplayName("Should handle switch flags correctly")
    void testSwitchFlags() {
        ExecutionResult<TestSource> result = execute("ban mqzen");
        assertSuccess(result);
        assertArgument(result, "target", "mqzen");
        assertFlag(result, "silent", false);
        assertFlag(result, "ip", false);
        assertArgument(result, "reason", "Breaking server laws");
    }

    @Test
    @DisplayName("Should handle activated switch flags")
    void testActivatedSwitchFlags() {
        ExecutionResult<TestSource> result = execute("ban mqzen -s");
        assertSuccess(result);
        assertArgument(result, "target", "mqzen");
        assertFlag(result, "silent", true);
        assertFlag(result, "ip", false);
    }

    @Test
    @DisplayName("Should handle multiple switch flags")
    void testMultipleSwitchFlags() {
        ExecutionResult<TestSource> result = execute("ban mqzen -s -ip");
        assertSuccess(result);
        assertArgument(result, "target", "mqzen");
        assertFlag(result, "silent", true);
        assertFlag(result, "ip", true);
    }

    @Test
    @DisplayName("Should handle value flags")
    void testValueFlags() {
        ExecutionResult<TestSource> result = execute("git commit -m \"Initial commit\"");
        assertSuccess(result);
        assertFlag(result, "message", "Initial commit");
    }

    @Test
    @DisplayName("Should handle mixed flags and arguments")
    void testMixedFlagsAndArguments() {
        ExecutionResult<TestSource> result = execute("ban mqzen -s 1d Cheating is bad");
        assertSuccess(result);
        assertArgument(result, "target", "mqzen");
        assertFlag(result, "silent", true);
        assertArgument(result, "duration", "1d");
        assertArgument(result, "reason", "Cheating is bad");
    }

    @Test
    @DisplayName("Should handle flag aliases")
    void testFlagAliases() {
        ExecutionResult<TestSource> result = execute("ban mqzen -silent");
        assertSuccess(result);
        assertArgument(result, "target", "mqzen");
        assertFlag(result, "silent", true);
    }

    @ParameterizedTest
    @CsvSource({
            "'ban mqzen -s', mqzen, true, false, permanent, 'Breaking server laws'",
            "'ban mqzen -ip', mqzen, false, true, permanent, 'Breaking server laws'",
            "'ban mqzen -s -ip', mqzen, true, true, permanent, 'Breaking server laws'",
            "'ban mqzen -ip -s', mqzen, true, true, permanent, 'Breaking server laws'"
    })
    @DisplayName("Should handle various flag combinations")
    void testVariousFlagCombinations(String commandLine, String expectedTarget, boolean expectedSilent,
            boolean expectedIp, String expectedDuration, String expectedReason) {
        ExecutionResult<TestSource> result = execute(commandLine);
        assertSuccess(result);
        assertArgument(result, "target", expectedTarget);
        assertFlag(result, "silent", expectedSilent);
        assertFlag(result, "ip", expectedIp);
        assertArgument(result, "duration", expectedDuration);
        assertArgument(result, "reason", expectedReason);
    }

    @Test
    @DisplayName("Should handle middle flag with arguments")
    void testMiddleFlagWithArguments() {
        ExecutionResult<TestSource> result = execute("ban2 mqzen -t 7d Cheating is not good");
        assertSuccess(result);
        assertArgument(result, "target", "mqzen");
        assertFlag(result, "time", "7d");
        assertArgument(result, "reason", "Cheating is not good");
    }
}
