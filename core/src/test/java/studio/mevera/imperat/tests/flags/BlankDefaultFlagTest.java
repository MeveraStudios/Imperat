package studio.mevera.imperat.tests.flags;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

/**
 * Regression: a blank {@link Default @Default} value on a value flag
 * (e.g. {@code @Default("")} or {@code @Default("  ")}) must not invoke
 * the input type's parser — most String parsers reject blank input with
 * {@code IllegalArgumentException("Input is empty")}, which previously
 * propagated to the caller and made the entire pathway fail.
 */
@DisplayName("Blank @Default flag values")
final class BlankDefaultFlagTest {

    public static final AtomicReference<String> CAPTURED = new AtomicReference<>();

    @Test
    @DisplayName("Empty @Default(\"\") on a value flag → null, no parse, no failure")
    void blankDefaultDoesNotInvokeParser() {
        TestImperat imperat = TestImperatConfig.builder().build();
        imperat.registerCommand(BlankDefaultCmd.class);

        CAPTURED.set("sentinel");
        ExecutionResult<TestCommandSource> r =
                imperat.execute(imperat.createDummySender(), "blankdef");

        assertFalse(r.hasFailed(),
                "Blank default must not crash the pathway: " + r.getError());
        assertNull(CAPTURED.get(),
                "Flag value must be null when @Default is blank and the flag is absent");
    }

    @RootCommand("blankdef")
    public static final class BlankDefaultCmd {
        @Execute
        public void run(TestCommandSource s,
                        @Flag({"output", "o"}) @Default("") String output) {
            CAPTURED.set(output);
        }
    }
}
