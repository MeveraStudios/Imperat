package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.tests.commands.MethodPermissionSuggestionCommand;

@DisplayName("Auto Completion Permission Tests")
class AutoCompletionPermissionTest {

    @Test
    @DisplayName("Should hide executable node suggestions when pathway permission is missing")
    void testExecutableNodeSuggestionUsesPathwayPermission() {
        TestImperat imperat = TestImperatConfig.builder()
                                      .permissionChecker((src, perm) -> perm == null || src.hasPermission(perm))
                                      .build();
        imperat.registerCommand(MethodPermissionSuggestionCommand.class);

        TestCommandSource deniedSource = new TestCommandSource(System.out);
        TestCommandSource allowedSource = new TestCommandSource(System.out).withPerm("permcomplete.restricted");

        var deniedSuggestions = imperat.autoComplete(deniedSource, "permcomplete ").join();
        Assertions.assertThat(deniedSuggestions)
                .contains("open")
                .doesNotContain("restricted");

        var allowedSuggestions = imperat.autoComplete(allowedSource, "permcomplete ").join();
        Assertions.assertThat(allowedSuggestions)
                .contains("open", "restricted");
    }
}
