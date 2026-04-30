package studio.mevera.imperat.tests.flags;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.DuplicateFlagException;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

/**
 * Repeated flag tokens for the same flag (e.g. {@code --foo bar --foo baz})
 * are a user error — the framework throws {@link DuplicateFlagException}
 * rather than silently overwriting the earlier binding.
 */
@DisplayName("Duplicate flag detection")
final class DuplicateFlagDetectionTest {

    @Test
    @DisplayName("Repeated value flag (--name) → DuplicateFlagException")
    void repeatedValueFlagFails() {
        TestImperat imperat = TestImperatConfig.builder().build();
        imperat.registerCommand(DupCmd.class);

        ExecutionResult<TestCommandSource> r = imperat.execute(
                imperat.createDummySender(),
                "dupcmd --output a --output b");

        assertTrue(r.hasFailed(), "Duplicate flag must surface as a failure");
        Throwable cause = r.getError();
        assertInstanceOf(DuplicateFlagException.class, cause,
                "Expected DuplicateFlagException, got: " + cause);
    }

    @Test
    @DisplayName("Repeated switch via primary + alias → DuplicateFlagException")
    void repeatedSwitchAcrossPrefixes() {
        TestImperat imperat = TestImperatConfig.builder().build();
        imperat.registerCommand(DupCmd.class);

        ExecutionResult<TestCommandSource> r = imperat.execute(
                imperat.createDummySender(),
                "dupcmd --verbose -v");

        assertTrue(r.hasFailed());
        assertInstanceOf(DuplicateFlagException.class, r.getError());
    }

    @Test
    @DisplayName("Single occurrence of each flag → success")
    void singleOccurrenceSucceeds() {
        TestImperat imperat = TestImperatConfig.builder().build();
        imperat.registerCommand(DupCmd.class);

        ExecutionResult<TestCommandSource> r = imperat.execute(
                imperat.createDummySender(),
                "dupcmd --output a -v");

        assertTrue(!r.hasFailed(),
                "Single-occurrence flags must succeed: " + r.getError());
    }

    @RootCommand("dupcmd")
    public static final class DupCmd {

        @Execute
        public void run(TestCommandSource s,
                @Flag({"output", "o"}) @Default("none") String output,
                @Switch({"verbose", "v"}) Boolean verbose) {
            // capture-only fixture
        }
    }
}
