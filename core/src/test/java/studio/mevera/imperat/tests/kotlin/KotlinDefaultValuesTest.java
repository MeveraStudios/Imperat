package studio.mevera.imperat.tests.kotlin;

import org.junit.jupiter.api.Test;
import studio.mevera.imperat.CommandParsingMode;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.commands.KotlinDefaultCommand;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class KotlinDefaultValuesTest {

    private static ExecutionResult<TestSource> executeWithCapture(
            TestImperat imperat,
            String commandLine,
            ByteArrayOutputStream out
    ) {
        TestSource source = new TestSource(new PrintStream(out));
        return imperat.execute(source, commandLine);
    }

    @Test
    void shouldUseKotlinDefaultWhenArgumentOmitted() {
        TestImperat imperat = TestImperatConfig.builder()
                .applyOnConfig(cfg -> cfg.setCommandParsingMode(CommandParsingMode.KOTLIN))
                .build();
        imperat.registerCommand(KotlinDefaultCommand.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExecutionResult<TestSource> result = executeWithCapture(imperat, "kdef", out);

        assertThat(result.hasFailed()).isFalse();
        assertThat(out.toString()).contains("input=hello");
    }

    @Test
    void shouldUseProvidedValueWhenArgumentPresent() {
        TestImperat imperat = TestImperatConfig.builder()
                .applyOnConfig(cfg -> cfg.setCommandParsingMode(CommandParsingMode.KOTLIN))
                .build();
        imperat.registerCommand(KotlinDefaultCommand.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExecutionResult<TestSource> result = executeWithCapture(imperat, "kdef hi", out);

        assertThat(result.hasFailed()).isFalse();
        assertThat(out.toString()).contains("input=hi");
    }
}
