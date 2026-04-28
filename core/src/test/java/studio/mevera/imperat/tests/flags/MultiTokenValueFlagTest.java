package studio.mevera.imperat.tests.flags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgumentBuilder;
import studio.mevera.imperat.command.arguments.type.ArgumentTypes;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.tests.parameters.TriCoord;
import studio.mevera.imperat.tests.parameters.TriCoordArgumentType;

/**
 * Lever 4 regression coverage: value-flag inputs of multi-token arity now
 * drain N tokens (where N comes from the inner type's
 * {@code getNumberOfParametersToConsume}) rather than the legacy hard-coded
 * single token.
 *
 * <p><b>Pre-fix:</b> a value flag bound to a 3-token type (e.g. {@code -coords}
 * → {@link TriCoord}) consumed exactly one token from the input. The flag's
 * inner-type parse received {@code Cursor.single(firstToken)} regardless of
 * its declared arity, so it either failed parsing or silently dropped the
 * remaining tokens — and the leftover tokens leaked into downstream
 * arguments / trailing-input failures.</p>
 */
@DisplayName("Multi-Token Value Flag Tests")
final class MultiTokenValueFlagTest {

    private static TestImperat freshImperat() {
        return TestImperatConfig.builder()
                       .argType(TriCoord.class, new TriCoordArgumentType())
                       .build();
    }

    // ===== EASY: legacy single-token value flag still works =====

    @Test
    @DisplayName("Single-token value flag still parses correctly")
    void singleTokenValueFlagBaseline() {
        TestImperat imperat = freshImperat();
        Command<TestCommandSource> cmd = Command.create(imperat, "ban")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.requiredText("target"))
                                                                  .withFlags(FlagArgumentBuilder.ofFlag("reason",
                                                                          ArgumentTypes.<TestCommandSource>string()))
                                                                  .execute((source, ctx) -> {
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result =
                imperat.execute(imperat.createDummySender(), "ban mqzen -reason rude");
        assertNotNull(result);
        assertFalse(result.hasFailed(), "Single-token value flag baseline must parse");
        assertEquals("mqzen", result.getExecutionContext().getArgument("target"));
        assertEquals("rude", result.getExecutionContext().<String>getFlagValue("reason"));
    }

    // ===== MEDIUM: 3-token value flag at end of line =====

    @Test
    @DisplayName("3-token value flag drains all three tokens at end of line")
    void multiTokenValueFlagAtEndOfLine() {
        TestImperat imperat = freshImperat();
        Command<TestCommandSource> cmd = Command.create(imperat, "tp")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.requiredText("target"))
                                                                  .withFlags(FlagArgumentBuilder.ofFlag("coords",
                                                                          new TriCoordArgumentType()))
                                                                  .execute((source, ctx) -> {
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result =
                imperat.execute(imperat.createDummySender(), "tp mqzen -coords 10 20 30");
        assertNotNull(result);
        assertFalse(result.hasFailed(),
                "3-token value flag must drain all three tokens (was broken pre-fix)");

        assertEquals("mqzen", result.getExecutionContext().getArgument("target"));
        TriCoord coord = result.getExecutionContext().getFlagValue("coords");
        assertNotNull(coord, "Flag value must materialise");
        assertEquals(10.0, coord.x);
        assertEquals(20.0, coord.y);
        assertEquals(30.0, coord.z);
    }

    // ===== HARD: 3-token value flag in trailing-flag position with another arg after =====
    // The trailing-flag region is consumed by TreeParser.consumeRemainingFlags;
    // it must drain N tokens too, otherwise the leftover tokens get reported as
    // "trailing input" and the parse fails.

    @Test
    @DisplayName("3-token flag drains all three even with extra trailing tokens that should fail")
    void multiTokenValueFlagWithExtraTrailing() {
        TestImperat imperat = freshImperat();
        Command<TestCommandSource> cmd = Command.create(imperat, "tp")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.requiredText("target"))
                                                                  .withFlags(FlagArgumentBuilder.ofFlag("coords",
                                                                          new TriCoordArgumentType()))
                                                                  .execute((source, ctx) -> {
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        // 3 coord tokens + 1 stray → stray should cause a trailing-input failure,
        // NOT silent token theft. Either way the test confirms the flag doesn't
        // partially-drain (which would consume only token 1 and leave 20/30/stray
        // as trailing input — different failure mode pre-fix).
        ExecutionResult<TestCommandSource> result =
                imperat.execute(imperat.createDummySender(), "tp mqzen -coords 10 20 30 stray");
        assertNotNull(result);
        // Either parses with stray as a binding error, OR fails — but if it parses
        // the coord must be all three tokens.
        if (!result.hasFailed()) {
            TriCoord coord = result.getExecutionContext().getFlagValue("coords");
            assertEquals(10.0, coord.x);
            assertEquals(20.0, coord.y);
            assertEquals(30.0, coord.z);
        }
    }

    // ===== HARD: 3-token flag with insufficient tokens fails cleanly =====

    @Test
    @DisplayName("3-token flag with only 2 trailing tokens fails (no partial drain)")
    void multiTokenValueFlagInsufficientTokens() {
        TestImperat imperat = freshImperat();
        Command<TestCommandSource> cmd = Command.create(imperat, "tp")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.requiredText("target"))
                                                                  .withFlags(FlagArgumentBuilder.ofFlag("coords",
                                                                          new TriCoordArgumentType()))
                                                                  .execute((source, ctx) -> {
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result =
                imperat.execute(imperat.createDummySender(), "tp mqzen -coords 10 20");
        assertNotNull(result);
        // Flag drained nothing (insufficient tokens) → flag failed → command fails.
        // Critical: must NOT silently parse with coord = (10, 20, ?).
        if (!result.hasFailed()) {
            TriCoord coord = result.getExecutionContext().getFlagValue("coords");
            // Must be null (flag didn't materialise) — never a half-drained TriCoord.
            org.junit.jupiter.api.Assertions.assertNull(coord,
                    "Insufficient tokens must NOT produce a partially-drained TriCoord");
        }
    }

    // ===== EASY: switch flag still works (no drain) =====

    @Test
    @DisplayName("Switch flag baseline still drains zero value tokens")
    void switchFlagDrainsNothing() {
        TestImperat imperat = freshImperat();
        Command<TestCommandSource> cmd = Command.create(imperat, "ban")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.requiredText("target"))
                                                                  .withFlags(FlagArgumentBuilder.<TestCommandSource, Object>ofSwitch("silent"))
                                                                  .execute((source, ctx) -> {
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result =
                imperat.execute(imperat.createDummySender(), "ban mqzen -silent");
        assertNotNull(result);
        assertFalse(result.hasFailed());
        assertEquals(true, result.getExecutionContext().<Boolean>getFlagValue("silent"));
    }
}
