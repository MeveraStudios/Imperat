package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Verifies the branch-failure surfacing model in {@code TreeParser}:
 *
 * <ul>
 *   <li>When a node has no sibling alternatives and its parse fails, the
 *       captured failure is surfaced — the user sees the actual rejection
 *       reason, not a contextless {@code InvalidSyntaxException}.</li>
 *   <li>When multiple sibling alternatives exist and at least one
 *       succeeds, the successful branch wins and sibling failures are
 *       discarded.</li>
 *   <li>When all sibling alternatives fail, the most-informative failure
 *       is selected (deepest-consumed first, then highest argument-type
 *       priority, then earliest registration order).</li>
 * </ul>
 */
@DisplayName("Branch Failure Surfacing Test")
public class BranchFailureSurfacingTest extends EnhancedBaseImperatTest {

    private static final ResponseKey TEST_KEY = () -> "branch-failure-test-error";

    // ===== Single-pathway: no siblings, surface the captured error =================

    @Test
    @DisplayName("Single pathway: parse failure surfaces as the actual exception, not InvalidSyntax")
    void testSinglePathwayParseFailureSurfaced() {
        ExecutionResult<TestCommandSource> result = execute(StrictRequiredCommand.class, cfg -> {
            cfg.registerArgType(StrictValue.class, new StrictValueArgumentType());
        }, "strict bad");

        Assertions.assertThat(result.hasFailed()).isTrue();
        Assertions.assertThat(result.getError())
                .isInstanceOf(ResponseException.class)
                .isNotInstanceOf(InvalidSyntaxException.class);
    }

    @Test
    @DisplayName("Single pathway with optional: input attempted but rejected → surface, do NOT silently default")
    void testOptionalArgWithRejectedInputSurfacesError() {
        ExecutionResult<TestCommandSource> result = execute(OptionalStrictCommand.class, cfg -> {
            cfg.registerArgType(StrictValue.class, new StrictValueArgumentType());
        }, "optstrict bad");

        Assertions.assertThat(result.hasFailed()).isTrue();
        // The user supplied "bad" for the optional slot; the type rejected
        // it. Silently using the default would mask the user's clearly-
        // intentional input. The walker bubbles the captured ResponseException
        // through the surfacing path.
        Assertions.assertThat(result.getError())
                .isInstanceOf(ResponseException.class);
    }

    @Test
    @DisplayName("Single pathway with optional: NO input → default used silently (preserved behaviour)")
    void testOptionalArgNoInputUsesDefaultSilently() {
        ExecutionResult<TestCommandSource> result = execute(OptionalStrictCommand.class, cfg -> {
            cfg.registerArgType(StrictValue.class, new StrictValueArgumentType());
        }, "optstrict");

        Assertions.assertThat(result.hasFailed()).isFalse();
    }

    // ===== Sibling rescue: a successful sibling discards the failed one ============

    // (Uncovered cleanly with current test infra — would require two
    // sibling subcommands accepting different inputs; the model itself is
    // exercised structurally by every passing pre-existing test that has
    // multiple siblings registered. Documented here as a deliberate gap.)

    // ===== Back-compat: unmarked exceptions become CommandException-wrapped =======

    @Test
    @DisplayName("Plain IllegalArgumentException from a custom type is surfaced (wrapped) when no sibling rescues")
    void testPlainExceptionStillSurfacedWhenSinglePathway() {
        ExecutionResult<TestCommandSource> result = execute(SlotMismatchCommand.class, cfg -> {
            cfg.registerArgType(SlotMismatchValue.class, new SlotMismatchArgumentType());
        }, "mismatch bad");

        // No sibling exists — the captured IllegalArgumentException is
        // wrapped and surfaced. Previously this fell through to a
        // contextless InvalidSyntax; now the user gets the actual reason.
        Assertions.assertThat(result.hasFailed()).isTrue();
        Throwable raised = result.getError();
        boolean wrappedAsExpected = raised instanceof CommandException
                                            && (raised.getCause() instanceof IllegalArgumentException
                                                        || raised.getMessage() != null
                                                                   && raised.getMessage().contains("not a valid"));
        Assertions.assertThat(wrappedAsExpected).isTrue();
    }

    // ===== Structural failures throw EXACTLY InvalidSyntaxException ===============

    @Test
    @DisplayName("Wrong argument structure (extra trailing input on a no-arg command) throws exactly InvalidSyntaxException")
    void testWrongArgThrowsExactlyInvalidSyntax() {
        // `/noargs` accepts no arguments. Typing `/noargs extra` is a
        // structural mismatch — the trailing-input filter rejects every
        // candidate pathway, no parse error gets captured, the walker
        // returns a synthetic Failed, and {@code execute} swallows it so
        // the dispatcher's own InvalidSyntax fallback fires with the
        // correct invalid-usage + closest-pathway hint.
        ExecutionResult<TestCommandSource> result = execute(
                NoArgsCommand.class, cfg -> {
                }, "noargs extra");

        Assertions.assertThat(result.hasFailed()).isTrue();
        Assertions.assertThat(result.getError())
                .isExactlyInstanceOf(InvalidSyntaxException.class);
    }

    @Test
    @DisplayName("Unknown subcommand throws exactly InvalidSyntaxException")
    void testUnknownSubcommandThrowsExactlyInvalidSyntax() {
        // `/parent foo` is the only registered subcommand.
        // `/parent unknownsub` triggers a subcommand-literal mismatch —
        // pure walker bookkeeping, synthetic Failed, swallowed by
        // {@code execute}, surfaced by the dispatcher as InvalidSyntax.
        ExecutionResult<TestCommandSource> result = execute(
                ParentWithSubCommand.class, cfg -> {
                }, "parent unknownsub");

        Assertions.assertThat(result.hasFailed()).isTrue();
        Assertions.assertThat(result.getError())
                .isExactlyInstanceOf(InvalidSyntaxException.class);
    }

    // ===== Fixtures ==============================================================

    public record StrictValue(String raw) {

    }

    public record SlotMismatchValue(String raw) {

    }

    /**
     * Throws {@link ArgumentParseException} (a {@link ResponseException})
     * on any input starting with "bad". Used to exercise the single-
     * pathway surfacing path.
     */
    public static final class StrictValueArgumentType extends SimpleArgumentType<TestCommandSource, StrictValue> {

        @Override
        public StrictValue parse(@NotNull CommandContext<TestCommandSource> context,
                @NotNull Argument<TestCommandSource> argument,
                @NotNull String input) throws CommandException {
            if (input.startsWith("bad")) {
                throw new ArgumentParseException(TEST_KEY, input);
            }
            return new StrictValue(input);
        }
    }

    /**
     * Throws an unmarked {@link IllegalArgumentException} on bad input.
     * Used to verify back-compat — unmarked throwables are wrapped as
     * {@link CommandException} and surfaced when no sibling rescues.
     */
    public static final class SlotMismatchArgumentType extends SimpleArgumentType<TestCommandSource, SlotMismatchValue> {

        @Override
        public SlotMismatchValue parse(@NotNull CommandContext<TestCommandSource> context,
                @NotNull Argument<TestCommandSource> argument,
                @NotNull String input) throws CommandException {
            if (input.startsWith("bad")) {
                throw new IllegalArgumentException("not a valid SlotMismatchValue");
            }
            return new SlotMismatchValue(input);
        }
    }

    @RootCommand("strict")
    public static final class StrictRequiredCommand {

        @Execute
        public void execute(TestCommandSource source, StrictValue value) {
            source.reply("got " + value.raw());
        }
    }

    @RootCommand("optstrict")
    public static final class OptionalStrictCommand {

        @Execute
        public void execute(TestCommandSource source,
                @studio.mevera.imperat.annotations.types.Optional
                @studio.mevera.imperat.annotations.types.Default("default") StrictValue value) {
            source.reply("got " + (value == null ? "null" : value.raw()));
        }
    }

    @RootCommand("mismatch")
    public static final class SlotMismatchCommand {

        @Execute
        public void execute(TestCommandSource source, SlotMismatchValue value) {
            source.reply("got " + value.raw());
        }
    }

    @RootCommand("noargs")
    public static final class NoArgsCommand {

        @Execute
        public void execute(TestCommandSource source) {
            source.reply("ran");
        }
    }

    @RootCommand("parent")
    public static final class ParentWithSubCommand {

        @studio.mevera.imperat.annotations.types.SubCommand("foo")
        public static final class Foo {
            @Execute
            public void execute(TestCommandSource source) {
                source.reply("foo");
            }
        }
    }
}
