package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lever 3 was investigated and found NOT to be a real bug — the existing
 * suggester at {@code TreeSuggester#tabComplete} already filters strictly
 * to the highest-scoring candidate's chain when computing suggestions.
 * This test class documents the current (correct) behaviour as a regression
 * lock so any future refactor that accidentally re-introduces sibling
 * bleed fails immediately.
 */
@DisplayName("Sibling-Bleed Regression Lock")
final class NoSiblingBleedRegressionTest {

    private static List<String> autocomplete(TestImperat imperat, String input) throws Exception {
        return imperat.autoComplete(imperat.createDummySender(), input).get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("After committing to one subcommand, the other's args do NOT leak in")
    void afterCommittingDoNotLeakSibling() throws Exception {
        // Two subcommands with DIFFERENT arg names. After committing to "open",
        // suggesting at the next position must NOT show args belonging to "close".
        TestImperat imperat = TestImperatConfig.builder().build();
        Command<TestCommandSource> cmd = Command.create(imperat, "menu")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .execute((src, ctx) -> {
                                                                  }))
                                                 .subCommand(Command.create(imperat, "open")
                                                                     .pathway(CommandPathway.<TestCommandSource>builder()
                                                                                      .arguments(Argument.<TestCommandSource>requiredText("openArg")
                                                                                                         .suggest("openA", "openB"))
                                                                                      .execute((src, ctx) -> {
                                                                                      }))
                                                                     .build())
                                                 .subCommand(Command.create(imperat, "close")
                                                                     .pathway(CommandPathway.<TestCommandSource>builder()
                                                                                      .arguments(Argument.<TestCommandSource>requiredText("closeArg")
                                                                                                         .suggest("closeX", "closeY"))
                                                                                      .execute((src, ctx) -> {
                                                                                      }))
                                                                     .build())
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        List<String> suggestions = autocomplete(imperat, "menu open ");
        Assertions.assertThat(suggestions)
                .as("Suggestions after 'menu open' must come from open's arg only")
                .contains("openA", "openB")
                .doesNotContain("closeX", "closeY");
    }

    @Test
    @DisplayName("Multiple variants on root: best candidate's children only")
    void multipleVariantsBestCandidateOnly() throws Exception {
        TestImperat imperat = TestImperatConfig.builder().build();
        // Two pathways at root level: <name> alone, OR <name> set <value>.
        // After typing "cfg foo set ", suggestions for the next position
        // should come from the "set" continuation — NOT from sibling
        // pathways that don't go through "set".
        Command<TestCommandSource> cmd = Command.create(imperat, "cfg")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.<TestCommandSource>requiredText("name"))
                                                                  .execute((src, ctx) -> {
                                                                  }))
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(
                                                                          Argument.<TestCommandSource>requiredText("name"),
                                                                          Argument.<TestCommandSource>requiredText("verb")
                                                                                  .suggest("set", "unset"),
                                                                          Argument.<TestCommandSource>requiredText("value")
                                                                                  .suggest("yes", "no")
                                                                  )
                                                                  .execute((src, ctx) -> {
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        List<String> suggestions = autocomplete(imperat, "cfg foo set ");
        Assertions.assertThat(suggestions)
                .as("Suggestions at value position must reflect the picked pathway")
                .contains("yes", "no");
    }
}
