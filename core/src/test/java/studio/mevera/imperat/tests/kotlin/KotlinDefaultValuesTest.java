package studio.mevera.imperat.tests.kotlin;

import org.junit.jupiter.api.Test;
import studio.mevera.imperat.CommandParsingMode;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.command.returns.BaseReturnResolver;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.commands.KotlinDefaultCommand;
import studio.mevera.imperat.tests.commands.KotlinReturnCommand;

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

    @Test
    void shouldHandleReturnValuesLikeMethodCommandExecutor() {
        TestImperat imperat = TestImperatConfig.builder()
                .applyOnConfig(cfg -> {
                    cfg.setCommandParsingMode(CommandParsingMode.KOTLIN);
                })
                .returnResolver(String.class, new BaseReturnResolver<TestSource, String>(String.class) {
                    @Override
                    public void handle(ExecutionContext<TestSource> context, MethodElement method, String value) {
                        context.source().reply("returned=" + value);
                    }
                })
                .build();
        imperat.registerCommand(KotlinReturnCommand.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExecutionResult<TestSource> result = executeWithCapture(imperat, "kret", out);

        assertThat(result.hasFailed()).isFalse();
        assertThat(out.toString()).contains("returned=ok");
    }
}
