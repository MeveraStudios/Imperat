package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;
import studio.mevera.imperat.tests.commands.NestedSwitchScopeCommand;
import studio.mevera.imperat.tests.parameters.TestPlayerParamType;

@DisplayName("Nested Switch Scope Completion Tests")
class NestedSwitchScopeCompletionTest extends EnhancedBaseImperatTest {

    private void registerNestedCommandTypes(ImperatConfig<TestCommandSource> cfg) {
        cfg.registerArgType(TestPlayer.class, new TestPlayerParamType());
    }

    @Test
    @DisplayName("Does not leak a subcommand switch into root suggestions")
    void rootSuggestionsShouldNotIncludeNestedSwitches() {
        var suggestions = tabComplete(NestedSwitchScopeCommand.class, this::registerNestedCommandTypes, "switchscope ");

        Assertions.assertThat(suggestions)
                .contains("sub1")
                .doesNotContain("-sub1switch", "-sub2switch");
    }

    @Test
    @DisplayName("Uses the last matched command path when suggesting switches")
    void subcommandSuggestionsShouldUseMatchedCommandFlags() {
        var suggestions = tabComplete(NestedSwitchScopeCommand.class, this::registerNestedCommandTypes, "switchscope sub1 ");

        Assertions.assertThat(suggestions)
                .contains("sub2", "-sub1switch")
                .doesNotContain("-sub2switch");
    }

    @Test
    @DisplayName("Continues suggesting descendants after consuming a switch")
    void completionShouldContinueAfterSwitchConsumption() {
        var suggestions = tabComplete(NestedSwitchScopeCommand.class, this::registerNestedCommandTypes, "switchscope sub1 -sub1switch ");

        Assertions.assertThat(suggestions)
                .contains("sub2");
    }

    @Test
    @DisplayName("Does not suggest a switch that is already present in the input")
    void completionShouldNotSuggestAlreadyUsedSwitch() {
        var suggestions = tabComplete(NestedSwitchScopeCommand.class, this::registerNestedCommandTypes, "switchscope sub1 -sub1switch ");

        Assertions.assertThat(suggestions)
                .contains("sub2")
                .doesNotContain("-sub1switch");
    }

    @Test
    @DisplayName("Filters switch suggestions by the active prefix")
    void completionShouldFilterSwitchSuggestionsByPrefix() {
        var suggestions = tabComplete(NestedSwitchScopeCommand.class, this::registerNestedCommandTypes, "switchscope sub1 -sub1");

        Assertions.assertThat(suggestions)
                .contains("-sub1switch")
                .doesNotContain("sub2", "-sub2switch");
    }

    @Test
    @DisplayName("Does not leak default pathway switches into a sibling executable pathway")
    void completionShouldNotSuggestDefaultPathwaySwitchForSiblingExecutable() {
        var suggestions = tabComplete(NestedSwitchScopeCommand.class, this::registerNestedCommandTypes, "switchscope sub1 0 ");

        Assertions.assertThat(suggestions)
                .doesNotContain("-sub1switch");
    }

    @Test
    @DisplayName("Suggests terminal pathway switches after consuming the final argument")
    void completionShouldSuggestLeafSwitchAfterFinalArgument() {
        var suggestions = tabComplete(NestedSwitchScopeCommand.class, this::registerNestedCommandTypes,
                "switchscope sub1 -sub1switch sub2 -sub2switch sub3 MQZEN ");

        Assertions.assertThat(suggestions)
                .contains("-sub3switch");
    }
}
