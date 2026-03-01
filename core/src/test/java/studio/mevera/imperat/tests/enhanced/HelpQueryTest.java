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
 * Tests for the help query system — verifying that {@code queryHelp} on the command tree
 * returns the correct set of {@link HelpEntry} entries for various commands.
 */
@DisplayName("Help Query Tests")
class HelpQueryTest extends EnhancedBaseImperatTest {

    // ── Utility ──────────────────────────────────────────────────────────────

    /**
     * Queries all help entries for the given root command name using default settings.
     */
    private HelpEntryList<TestSource> queryHelp(String commandName) {
        return queryHelp(commandName, HelpQuery.<TestSource>builder().build());
    }

    /**
     * Queries help entries for the given root command name using a custom query.
     */
    private HelpEntryList<TestSource> queryHelp(String commandName, HelpQuery<TestSource> query) {
        Command<TestSource> cmd = IMPERAT.getCommand(commandName);
        Assertions.assertThat(cmd)
                .as("Command '%s' should be registered", commandName)
                .isNotNull();
        assert cmd != null;
        return cmd.tree().queryHelp(query);
    }

    /**
     * Collects the formatted usage strings from a {@link HelpEntryList}.
     */
    private List<String> formattedUsages(HelpEntryList<TestSource> entries) {
        List<String> usages = new ArrayList<>();
        for (HelpEntry<TestSource> entry : entries) {
            usages.add(entry.getPathway().formatted());
        }
        return usages;
    }

    // ── Basic help query tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("Basic Queries")
    class BasicQueries {

        @Test
        @DisplayName("queryHelp should return non-empty entries for a command with usages")
        void shouldReturnEntriesForCommandWithUsages() {
            HelpEntryList<TestSource> entries = queryHelp("test");

            Assertions.assertThat(entries.size())
                    .as("Command 'test' should have at least one help entry")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("Every help entry should have a non-null pathway")
        void everyEntryShouldHavePathway() {
            HelpEntryList<TestSource> entries = queryHelp("test");

            for (HelpEntry<TestSource> entry : entries) {
                Assertions.assertThat(entry.getPathway())
                        .as("Each help entry must have a non-null pathway")
                        .isNotNull();
            }
        }

        @Test
        @DisplayName("Every help entry's node should be executable")
        void everyEntryShouldBeExecutable() {
            HelpEntryList<TestSource> entries = queryHelp("test");

            for (HelpEntry<TestSource> entry : entries) {
                Assertions.assertThat(entry.getNode().isExecutable())
                        .as("Help entry node '%s' must be executable", entry.getNode().format())
                        .isTrue();
            }
        }
    }

    // ── Limit & depth tests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Limits and Depth")
    class LimitsAndDepth {

        @Test
        @DisplayName("queryHelp with limit=0 should return empty list")
        void limitZeroShouldReturnEmpty() {
            HelpQuery<TestSource> query = HelpQuery.<TestSource>builder()
                                                  .limit(0)
                                                  .build();
            HelpEntryList<TestSource> entries = queryHelp("test", query);

            Assertions.assertThat(entries.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("queryHelp with limit=1 should return at most 1 entry")
        void limitOneShouldReturnAtMostOne() {
            HelpQuery<TestSource> query = HelpQuery.<TestSource>builder()
                                                  .limit(1)
                                                  .build();
            HelpEntryList<TestSource> entries = queryHelp("test", query);

            Assertions.assertThat(entries.size())
                    .isLessThanOrEqualTo(1);
        }

        @Test
        @DisplayName("queryHelp with maxDepth=0 should only return root-level entries")
        void maxDepthZeroShouldReturnOnlyRoot() {
            HelpQuery<TestSource> query = HelpQuery.<TestSource>builder()
                                                  .maxDepth(0)
                                                  .build();
            HelpEntryList<TestSource> entries = queryHelp("test", query);

            for (HelpEntry<TestSource> entry : entries) {
                Assertions.assertThat(entry.getNode().getDepth())
                        .as("All entries should be at depth <= 0")
                        .isLessThanOrEqualTo(0);
            }
        }
    }

    // ── Duplicate prevention ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Duplicate Prevention")
    class DuplicatePrevention {

        @Test
        @DisplayName("queryHelp should not contain duplicate entries")
        void shouldNotContainDuplicates() {
            HelpEntryList<TestSource> entries = queryHelp("test");

            List<String> usages = formattedUsages(entries);
            Assertions.assertThat(usages)
                    .as("Help entries should not have duplicates")
                    .doesNotHaveDuplicates();
        }
    }

    // ── Multi-command structural tests ───────────────────────────────────────

    @Nested
    @DisplayName("Structure")
    class Structure {

        @Test
        @DisplayName("A command with subcommands should include entries for the subcommands")
        void shouldIncludeSubcommandEntries() {
            HelpEntryList<TestSource> entries = queryHelp("secrettest");

            List<String> usages = formattedUsages(entries);

            // 'visible' and 'public' are non-secret subcommands, so they must appear
            Assertions.assertThat(usages.stream().anyMatch(u -> u.contains("visible")))
                    .as("Help should include 'visible' subcommand entries")
                    .isTrue();

            Assertions.assertThat(usages.stream().anyMatch(u -> u.contains("public")))
                    .as("Help should include 'public' subcommand entries")
                    .isTrue();
        }

        @Test
        @DisplayName("A command's help entries should cover deep children")
        void shouldIncludeDeepChildren() {
            HelpEntryList<TestSource> entries = queryHelp("secrettest");

            List<String> usages = formattedUsages(entries);

            Assertions.assertThat(usages.stream().anyMatch(u -> u.contains("child")))
                    .as("Help should include 'visible child' deep entry")
                    .isTrue();
        }
    }
}


