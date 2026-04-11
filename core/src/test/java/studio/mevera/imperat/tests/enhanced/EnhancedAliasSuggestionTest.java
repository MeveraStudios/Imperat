package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.tests.commands.SubcommandAliasSuggestionCommand;

@DisplayName("Enhanced Alias Suggestion Tests")
class EnhancedAliasSuggestionTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Should suggest all subcommand aliases in native completion")
    void testAllSubcommandAliasesAreSuggested() {
        var results = tabComplete(SubcommandAliasSuggestionCommand.class, cfg -> {
        }, "aliascmd ");

        Assertions.assertThat(results)
                .containsExactlyInAnyOrder("manage", "m", "mgr", "profile", "p", "prof")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should filter native subcommand aliases by prefix without dropping later aliases")
    void testSubcommandAliasesRemainVisibleWithPrefix() {
        var results = tabComplete(SubcommandAliasSuggestionCommand.class, cfg -> {
        }, "aliascmd m");

        Assertions.assertThat(results)
                .containsExactly("manage", "m", "mgr");
    }
}
