package studio.mevera.imperat.tests.contextresolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;
import studio.mevera.imperat.tests.enhanced.EnhancedBaseImperatTest;
import studio.mevera.imperat.tests.parameters.TestPlayerParamType;


@DisplayName("CommandContext Resolving Test")
public class ContextResolvingTest extends EnhancedBaseImperatTest {

    @Test
    public void testBasic() {
        var res = execute("ctx sub2");
        assertThat(res)
                .isSuccessful()
                .hasContextArgumentOf(SomeData.class, (data)-> data.data().equals("test"));
    }

    @Test
    @DisplayName("Should parse non-first parameters from input even when their type also has a source resolver")
    void testSourceResolvedTypeAfterFirstParameterStillParsesAsArgument() {
        var res = execute(SourceResolvedArgumentCommand.class, cfg -> {
            // v4: SourceProviderRegistry deleted — domain-typed source views
            // come from ContextArgumentProvider instead. This regression test
            // verifies that an arg-type registered for the same Java type
            // takes precedence at parse-position-1+ over the context-derived
            // form (which is now opt-in via ContextArgumentProvider).
            cfg.registerContextArgumentProvider(TestPlayer.class, (ctx, p) -> new TestPlayer("source"));
            cfg.registerArgType(TestPlayer.class, new TestPlayerParamType());
        }, "srcarg target123");

        assertThat(res)
                .isSuccessful()
                .hasArgument("target", new TestPlayer("target123"));
    }

    @RootCommand("srcarg")
    public static final class SourceResolvedArgumentCommand {

        @Execute
        public void execute(TestCommandSource source, TestPlayer target) {
            source.reply(target.toString());
        }
    }

}
