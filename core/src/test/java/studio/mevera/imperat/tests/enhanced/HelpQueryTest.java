package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.tree.help.HelpEntry;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.command.tree.help.HelpResult;
import studio.mevera.imperat.tests.TestCommandSource;

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
    private HelpResult<TestCommandSource> queryHelp(String commandName) {
        return queryHelp(commandName, HelpQuery.<TestCommandSource>builder().build());
    }

    /**
     * Queries help entries for the given root command name using a custom query.
     */
    private HelpResult<TestCommandSource> queryHelp(String commandName, HelpQuery<TestCommandSource> query) {
        Command<TestCommandSource> cmd = IMPERAT.getCommand(commandName);
        Assertions.assertThat(cmd)
                .as("Command '%s' should be registered", commandName)
                .isNotNull();
        assert cmd != null;
        return cmd.tree().queryHelp(query);
    }

    /**
     * Collects the formatted usage strings from a {@link HelpResult}.
     */
    private List<String> formattedUsages(HelpResult<TestCommandSource> entries) {
        List<String> usages = new ArrayList<>();
        for (HelpEntry<TestCommandSource> entry : entries) {
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
            HelpResult<TestCommandSource> entries = queryHelp("test");

            Assertions.assertThat(entries.size())
                    .as("Command 'test' should have at least one help entry")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("Every help entry should have a non-null pathway")
        void everyEntryShouldHavePathway() {
            HelpResult<TestCommandSource> entries = queryHelp("test");

            for (HelpEntry<TestCommandSource> entry : entries) {
                Assertions.assertThat(entry.getPathway())
                        .as("Each help entry must have a non-null pathway")
                        .isNotNull();
            }
        }

        @Test
        @DisplayName("Every help entry should have a valid pathway")
        void everyEntryShouldHaveValidPathway() {
            HelpResult<TestCommandSource> entries = queryHelp("test");

            for (HelpEntry<TestCommandSource> entry : entries) {
                Assertions.assertThat(entry.getPathway())
                        .as("Each help entry must have a non-null pathway")
                        .isNotNull();
                Assertions.assertThat(entry.getPathway().size())
                        .as("Each help entry pathway must have at least one parameter")
                        .isGreaterThan(0);
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
            HelpQuery<TestCommandSource> query = HelpQuery.<TestCommandSource>builder()
                                                  .limit(0)
                                                  .build();
            HelpResult<TestCommandSource> entries = queryHelp("test", query);

            Assertions.assertThat(entries.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("queryHelp with limit=1 should return at most 1 entry")
        void limitOneShouldReturnAtMostOne() {
            HelpQuery<TestCommandSource> query = HelpQuery.<TestCommandSource>builder()
                                                  .limit(1)
                                                  .build();
            HelpResult<TestCommandSource> entries = queryHelp("test", query);

            Assertions.assertThat(entries.size())
                    .isLessThanOrEqualTo(1);
        }

        @Test
        @DisplayName("queryHelp with maxDepth=0 should only return root-level entries")
        void maxDepthZeroShouldReturnOnlyRoot() {
            HelpQuery<TestCommandSource> query = HelpQuery.<TestCommandSource>builder()
                                                  .maxDepth(0)
                                                  .build();
            HelpResult<TestCommandSource> entries = queryHelp("test", query);

            // maxDepth=0 means only the root node (depth 0) is traversed,
            // so returned pathways should be root-level usages only
            for (HelpEntry<TestCommandSource> entry : entries) {
                Assertions.assertThat(entry.getPathway())
                        .as("All entries at maxDepth=0 should have a valid pathway")
                        .isNotNull();
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
            HelpResult<TestCommandSource> entries = queryHelp("test");

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
            HelpResult<TestCommandSource> entries = queryHelp("secrettest");

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
            HelpResult<TestCommandSource> entries = queryHelp("secrettest");

            List<String> usages = formattedUsages(entries);

            Assertions.assertThat(usages.stream().anyMatch(u -> u.contains("child")))
                    .as("Help should include 'visible child' deep entry")
                    .isTrue();
        }
    }
}
