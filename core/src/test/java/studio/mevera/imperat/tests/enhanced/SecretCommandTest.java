package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.tests.TestSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests that secret commands are properly hidden from tab completion suggestions
 * while still being executable.
 * <p>
 * Command structure:
 * <pre>
 *   /secrettest                          — root (executable)
 *     visible                            — normal subcommand
 *       child &lt;arg&gt;                     — normal child (suggests: alpha, beta)
 *     hidden                             — @Secret subcommand
 *       deep &lt;val&gt;                      — child of secret (suggests: x, y, z)
 *     public &lt;name&gt;                     — normal subcommand (suggests: alice, bob)
 * </pre>
 */
@DisplayName("Secret Command Suggestion Tests")
class SecretCommandTest extends EnhancedBaseImperatTest {

    // =========================================================================
    // Tab Completion — Secret subcommands should be hidden
    // =========================================================================

    @Nested
    @DisplayName("Tab Completion Hiding")
    class TabCompletionHiding {

        @Test
        @DisplayName("Root-level suggestions should NOT include the secret subcommand")
        void rootSuggestionsShouldExcludeSecret() {
            List<String> suggestions = tabComplete("secrettest ");

            Assertions.assertThat(suggestions)
                    .contains("visible", "public")
                    .doesNotContain("hidden");
        }

        @Test
        @DisplayName("Root-level suggestions with partial prefix should NOT include secret")
        void rootSuggestionsWithPrefixShouldExcludeSecret() {
            // Typing 'h' could match 'hidden' — but it's secret, so no
            List<String> suggestions = tabComplete("secrettest h");

            Assertions.assertThat(suggestions)
                    .doesNotContain("hidden");
        }

        @Test
        @DisplayName("Suggestions inside a secret subcommand should be empty")
        void suggestionsInsideSecretShouldBeEmpty() {
            // Even after typing the secret subcommand name, no deeper suggestions
            List<String> suggestions = tabComplete("secrettest hidden ");

            Assertions.assertThat(suggestions)
                    .isEmpty();
        }

        @Test
        @DisplayName("Deep suggestions inside a secret subcommand should be empty")
        void deepSuggestionsInsideSecretShouldBeEmpty() {
            // Typing deeper into the secret tree should produce nothing
            List<String> suggestions = tabComplete("secrettest hidden deep ");

            Assertions.assertThat(suggestions)
                    .doesNotContain("x", "y", "z");
        }

        @Test
        @DisplayName("Non-secret subcommand suggestions should still work normally")
        void nonSecretSubcommandSuggestionsShouldWork() {
            List<String> suggestions = tabComplete("secrettest visible ");

            Assertions.assertThat(suggestions)
                    .contains("child");
        }

        @Test
        @DisplayName("Non-secret child argument suggestions should still work normally")
        void nonSecretChildArgSuggestionsShouldWork() {
            List<String> suggestions = tabComplete("secrettest visible child ");

            Assertions.assertThat(suggestions)
                    .contains("alpha", "beta");
        }

        @Test
        @DisplayName("Non-secret 'public' subcommand arg suggestions should work normally")
        void publicSubcommandArgSuggestionsShouldWork() {
            List<String> suggestions = tabComplete("secrettest public ");

            Assertions.assertThat(suggestions)
                    .contains("alice", "bob");
        }
    }

    // =========================================================================
    // Execution — Secret commands should still be executable
    // =========================================================================

    @Nested
    @DisplayName("Execution of Secret Commands")
    class ExecutionOfSecretCommands {

        @Test
        @DisplayName("Secret subcommand should still execute successfully")
        void secretSubcommandShouldExecute() {
            assertThat(execute("secrettest hidden"))
                    .isSuccessful();
        }

        @Test
        @DisplayName("Deep child of secret subcommand should still execute successfully")
        void deepSecretChildShouldExecute() {
            assertThat(execute("secrettest hidden deep someValue"))
                    .isSuccessful()
                    .hasArgument("val", "someValue");
        }

        @Test
        @DisplayName("Non-secret subcommand should still execute successfully")
        void nonSecretSubcommandShouldExecute() {
            assertThat(execute("secrettest visible"))
                    .isSuccessful();
        }

        @Test
        @DisplayName("Public subcommand with argument should execute successfully")
        void publicSubcommandShouldExecute() {
            assertThat(execute("secrettest public alice"))
                    .isSuccessful()
                    .hasArgument("name", "alice");
        }
    }

    // =========================================================================
    // Help Entries — Secret commands should be excluded from help
    // =========================================================================

    @Nested
    @DisplayName("Help Entry Hiding")
    class HelpEntryHiding {

        private HelpEntryList<TestSource> querySecretTestHelp() {
            Command<TestSource> cmd = IMPERAT.getCommand("secrettest");
            Assertions.assertThat(cmd).as("Command 'secrettest' must be registered").isNotNull();
            assert cmd != null;
            HelpQuery<TestSource> query = HelpQuery.<TestSource>builder().build();
            return cmd.tree().queryHelp(query);
        }

        private List<String> formattedUsages(HelpEntryList<TestSource> entries) {
            List<String> usages = new ArrayList<>();
            for (HelpEntry<TestSource> entry : entries) {
                usages.add(entry.getPathway().formatted());
            }
            return usages;
        }

        @Test
        @DisplayName("Help entries should NOT include the secret subcommand 'hidden'")
        void helpShouldExcludeSecretSubcommand() {
            List<String> usages = formattedUsages(querySecretTestHelp());

            Assertions.assertThat(usages)
                    .as("No help entry should contain the secret 'hidden' subcommand")
                    .noneMatch(u -> u.contains("hidden"));
        }

        @Test
        @DisplayName("Help entries should NOT include children of the secret subcommand")
        void helpShouldExcludeSecretChildren() {
            List<String> usages = formattedUsages(querySecretTestHelp());

            // 'deep' is a child of the secret 'hidden' — it must also be hidden
            Assertions.assertThat(usages)
                    .as("No help entry should contain 'deep' (child of secret 'hidden')")
                    .noneMatch(u -> u.contains("deep"));
        }

        @Test
        @DisplayName("Help entries SHOULD include non-secret subcommands")
        void helpShouldIncludeNonSecretSubcommands() {
            List<String> usages = formattedUsages(querySecretTestHelp());

            Assertions.assertThat(usages.stream().anyMatch(u -> u.contains("visible")))
                    .as("Help should include 'visible' subcommand")
                    .isTrue();

            Assertions.assertThat(usages.stream().anyMatch(u -> u.contains("public")))
                    .as("Help should include 'public' subcommand")
                    .isTrue();
        }

        @Test
        @DisplayName("Help entries should include the visible child 'child'")
        void helpShouldIncludeVisibleChild() {
            List<String> usages = formattedUsages(querySecretTestHelp());

            Assertions.assertThat(usages.stream().anyMatch(u -> u.contains("child")))
                    .as("Help should include 'visible child' entry")
                    .isTrue();
        }

        @Test
        @DisplayName("Help entry count should exclude all secret entries")
        void helpEntryCountShouldExcludeSecret() {
            HelpEntryList<TestSource> entries = querySecretTestHelp();

            // secrettest has: root default, visible, visible child <arg>, public <name>
            // hidden and hidden deep <val> should be excluded
            // So we expect NO entry with 'hidden' anywhere
            List<String> usages = formattedUsages(entries);
            long secretCount = usages.stream().filter(u -> u.contains("hidden")).count();

            Assertions.assertThat(secretCount)
                    .as("There should be zero help entries referencing 'hidden'")
                    .isZero();
        }
    }
}

