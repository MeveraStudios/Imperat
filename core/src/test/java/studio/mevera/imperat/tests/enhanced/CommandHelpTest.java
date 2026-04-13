package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.util.TypeWrap;

import java.util.List;

@DisplayName("CommandHelpTest")
public class CommandHelpTest extends EnhancedBaseImperatTest {

    private TestImperat imperat;

    @BeforeEach
    void setupImperat() {
        imperat = TestImperatConfig.builder()
                          .sourceProvider(TestCommandSource.class, (source, ctx) -> source)
                          .contextArgumentProvider(new TypeWrap<CommandHelp<TestCommandSource>>() {
                          }.getType(), (ctx, pe) -> CommandHelp.create(ctx))
                          .build();
        imperat.registerCommand(HelpCommand.class);
    }

    @Test
    @DisplayName("Should display help properly in flat style")
    public void testFlatHelp() {
        TestCommandSource source = new TestCommandSource(System.out);

        var execResult = imperat.execute(source, "helptest");
        Assertions.assertThat(execResult.hasFailed()).isFalse();
        Assertions.assertThat(execResult.getExecutionContext()).isNotNull();

        List<String> lines = CommandHelp.create(execResult.getExecutionContext())
                                     .render(
                                             HelpQuery.<TestCommandSource>builder()
                                                     .filter(pathway -> !pathway.isDefault())
                                                     .build(),
                                             (context, result) -> List.of(
                                                     "======== Command Help ========",
                                                     "entries=" + result.size(),
                                                     "============================="
                                             )
                                     );

        Assertions.assertThat(lines)
                .containsExactly(
                        "======== Command Help ========",
                        "entries=1",
                        "============================="
                );
    }

    @RootCommand("helptest")
    public static final class HelpCommand {

        @Execute
        public void root(TestCommandSource source) {
        }

        @Execute
        @SubCommand("child")
        @Description("Child command.")
        public void child(TestCommandSource source) {
        }
    }
}
