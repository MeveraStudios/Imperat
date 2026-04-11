package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.tests.commands.SuggestionFallbackCommands;

@DisplayName("Enhanced Suggestion Fallback Tests")
class EnhancedSuggestionFallbackTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Should keep the fallback suggestion provider empty by default")
    void testFallbackSuggestionProviderDefaultsToEmpty() {
        TestImperat imperat = TestImperatConfig.builder().build();
        imperat.registerCommand(SuggestionFallbackCommands.class);

        var suggestions = imperat.autoComplete(SOURCE, "fallbackcmd ").join();

        Assertions.assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("Should use the builder-configured fallback suggestion provider when primary suggestions are empty")
    void testFallbackSuggestionProviderCanBeConfiguredThroughBuilder() {
        TestImperat imperat = TestImperatConfig.builder()
                                     .fallbackSuggestionProvider(SuggestionProvider.staticSuggestions("fallback-one", "fallback-two"))
                                     .build();
        imperat.registerCommand(SuggestionFallbackCommands.class);

        var suggestions = imperat.autoComplete(SOURCE, "fallbackcmd ").join();

        Assertions.assertThat(suggestions).containsExactly("fallback-one", "fallback-two");
    }

    @Test
    @DisplayName("Should not use the fallback suggestion provider when primary suggestions exist")
    void testFallbackSuggestionProviderDoesNotOverridePrimarySuggestions() {
        TestImperat imperat = TestImperatConfig.builder()
                                     .fallbackSuggestionProvider(SuggestionProvider.staticSuggestions("fallback-one", "fallback-two"))
                                     .build();
        imperat.registerCommand(SuggestionFallbackCommands.class);

        var suggestions = imperat.autoComplete(SOURCE, "primarycmd ").join();

        Assertions.assertThat(suggestions)
                .containsExactly("primary-one", "primary-two")
                .doesNotContain("fallback-one", "fallback-two");
    }
}
