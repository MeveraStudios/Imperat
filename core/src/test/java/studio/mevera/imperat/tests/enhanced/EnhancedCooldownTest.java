package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

import java.util.concurrent.TimeUnit;

/**
 * Tests for the cooldown system.
 *
 * <p>The cooldown is enforced in the post-process event listener inside
 * {@code BaseImperat}. When a pathway has a {@code CooldownRecord} set,
 * the handler tracks the last execution time per source.  On the second
 * invocation within the cooldown window the system throws a
 * {@link ResponseException} with {@link ResponseKey#COOLDOWN}.</p>
 */
@DisplayName("Cooldown Feature Tests")
class EnhancedCooldownTest extends EnhancedBaseImperatTest {

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers – each test gets its own Imperat + Command to avoid cross-talk
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Builds a fresh {@link TestImperat} with a single command that has
     * a cooldown on its default pathway.
     */
    private static TestImperat buildImperatWithCooldownCommand(
            String cmdName,
            long cooldownValue,
            TimeUnit cooldownUnit,
            String cooldownPermission
    ) {
        TestImperat imperat = TestImperatConfig.builder().build();

        Command<TestCommandSource> cmd = Command.<TestCommandSource>create(imperat, cmdName)
                                          .defaultExecution((source, context) -> source.reply("executed " + cmdName))
                                          .pathway(
                                                  CommandPathway.<TestCommandSource>builder()
                                                          .parameters(Argument.requiredText("target"))
                                                          .cooldown(cooldownValue, cooldownUnit, cooldownPermission)
                                                          .execute((source, context) -> {
                                                              String target = context.getArgument("target");
                                                              source.reply(cmdName + " target=" + target);
                                                          })
                                          )
                                          .build();

        imperat.registerSimpleCommand(cmd);
        return imperat;
    }

    /**
     * Builds a fresh {@link TestImperat} with a command that has a cooldown
     * on its default (no-arg) pathway.
     */
    private static TestImperat buildImperatWithDefaultCooldown(
            String cmdName,
            long cooldownValue,
            TimeUnit cooldownUnit
    ) {
        TestImperat imperat = TestImperatConfig.builder().build();

        Command<TestCommandSource> cmd = Command.<TestCommandSource>create(imperat, cmdName)
                                          .pathway(
                                                  CommandPathway.<TestCommandSource>builder()
                                                          .cooldown(cooldownValue, cooldownUnit)
                                                          .execute((source, context) -> source.reply("executed " + cmdName))
                                          )
                                          .build();

        imperat.registerSimpleCommand(cmd);
        return imperat;
    }

    /**
     * Builds an Imperat with a command that has subcommands, each with its own cooldown.
     */
    private static TestImperat buildImperatWithSubcommandCooldowns() {
        TestImperat imperat = TestImperatConfig.builder().build();

        Command<TestCommandSource> sub = Command.<TestCommandSource>create(imperat, "action")
                                          .pathway(
                                                  CommandPathway.<TestCommandSource>builder()
                                                          .parameters(Argument.requiredText("value"))
                                                          .cooldown(10, TimeUnit.SECONDS)
                                                          .execute((source, context) -> {
                                                              source.reply("action value=" + context.getArgument("value"));
                                                          })
                                          )
                                          .build();

        Command<TestCommandSource> root = Command.<TestCommandSource>create(imperat, "coolsub")
                                           .defaultExecution((source, context) -> source.reply("coolsub default"))
                                           .subCommand(sub)
                                           .build();

        imperat.registerSimpleCommand(root);
        return imperat;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Basic cooldown enforcement
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Basic Cooldown Enforcement")
    class BasicCooldown {

        @Test
        @DisplayName("First execution should always succeed")
        void firstExecutionShouldSucceed() {
            TestImperat imperat = buildImperatWithCooldownCommand("cd1", 10, TimeUnit.SECONDS, null);
            TestCommandSource source = new TestCommandSource(System.out);

            ExecutionResult<TestCommandSource> result = imperat.execute(source, "cd1 player1");
            assertThat(result).isSuccessful();
        }

        @Test
        @DisplayName("Second immediate execution should fail with cooldown error")
        void secondImmediateExecutionShouldFail() {
            TestImperat imperat = buildImperatWithCooldownCommand("cd2", 10, TimeUnit.SECONDS, null);
            TestCommandSource source = new TestCommandSource(System.out);

            // First execution — succeeds
            ExecutionResult<TestCommandSource> first = imperat.execute(source, "cd2 player1");
            assertThat(first).isSuccessful();

            // Second execution — should be blocked by cooldown
            ExecutionResult<TestCommandSource> second = imperat.execute(source, "cd2 player1");
            assertThat(second).hasFailed();
        }

        @Test
        @DisplayName("Cooldown should apply per-source — different sources should not block each other")
        void cooldownShouldBePerSource() {
            TestImperat imperat = buildImperatWithCooldownCommand("cd3", 10, TimeUnit.SECONDS, null);
            TestCommandSource source1 = new TestCommandSource(System.out);
            TestCommandSource source2 = new TestCommandSource(System.out) {
                @Override
                public String name() {
                    return "PLAYER2";
                }
            };

            // CommandSource 1 executes
            ExecutionResult<TestCommandSource> r1 = imperat.execute(source1, "cd3 target1");
            assertThat(r1).isSuccessful();

            // CommandSource 2 should still be able to execute
            ExecutionResult<TestCommandSource> r2 = imperat.execute(source2, "cd3 target1");
            assertThat(r2).isSuccessful();
        }

        @Test
        @DisplayName("Default (no-arg) pathway with cooldown should block repeat execution")
        void defaultPathwayCooldownShouldBlock() {
            TestImperat imperat = buildImperatWithDefaultCooldown("cd4", 10, TimeUnit.SECONDS);
            TestCommandSource source = new TestCommandSource(System.out);

            ExecutionResult<TestCommandSource> first = imperat.execute(source, "cd4");
            assertThat(first).isSuccessful();

            ExecutionResult<TestCommandSource> second = imperat.execute(source, "cd4");
            assertThat(second).hasFailed();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Cooldown expiry
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cooldown Expiry")
    class CooldownExpiry {

        @Test
        @DisplayName("After cooldown expires, execution should succeed again")
        void afterCooldownExpiresExecutionShouldSucceed() throws InterruptedException {
            // Use a very short cooldown (1 second) so the test doesn't stall
            TestImperat imperat = buildImperatWithCooldownCommand("cd5", 1, TimeUnit.SECONDS, null);
            TestCommandSource source = new TestCommandSource(System.out);

            ExecutionResult<TestCommandSource> first = imperat.execute(source, "cd5 player1");
            assertThat(first).isSuccessful();

            // Wait for cooldown to expire
            Thread.sleep(1100);

            ExecutionResult<TestCommandSource> second = imperat.execute(source, "cd5 player1");
            assertThat(second).isSuccessful();
        }

        @Test
        @DisplayName("Execution within cooldown period should still fail")
        void executionWithinCooldownPeriodShouldFail() throws InterruptedException {
            TestImperat imperat = buildImperatWithCooldownCommand("cd6", 2, TimeUnit.SECONDS, null);
            TestCommandSource source = new TestCommandSource(System.out);

            ExecutionResult<TestCommandSource> first = imperat.execute(source, "cd6 target");
            assertThat(first).isSuccessful();

            // Wait less than cooldown
            Thread.sleep(500);

            ExecutionResult<TestCommandSource> second = imperat.execute(source, "cd6 target");
            assertThat(second).hasFailed();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Cooldown permission bypass
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cooldown Permission Bypass")
    class CooldownPermissionBypass {

        @Test
        @DisplayName("CommandSource with cooldown-bypass permission should not be blocked")
        void sourceWithBypassPermissionShouldNotBeBlocked() {
            String bypassPerm = "cooldown.bypass";
            TestImperat imperat = TestImperatConfig.builder()
                                          .permissionChecker((src, perm) -> perm == null || src.hasPermission(perm))
                                          .build();

            Command<TestCommandSource> cmd = Command.<TestCommandSource>create(imperat, "cd7")
                                              .defaultExecution((source, context) -> source.reply("cd7 default"))
                                              .pathway(
                                                      CommandPathway.<TestCommandSource>builder()
                                                              .parameters(Argument.requiredText("target"))
                                                              .cooldown(10, TimeUnit.SECONDS, bypassPerm)
                                                              .execute((source, context) -> source.reply(
                                                                      "cd7 target=" + context.getArgument("target")))
                                              )
                                              .build();

            imperat.registerSimpleCommand(cmd);

            TestCommandSource source = new TestCommandSource(System.out).withPerm(bypassPerm);

            // First execution
            ExecutionResult<TestCommandSource> first = imperat.execute(source, "cd7 player1");
            assertThat(first).isSuccessful();

            // Second execution — source has bypass permission, should succeed
            ExecutionResult<TestCommandSource> second = imperat.execute(source, "cd7 player1");
            assertThat(second).isSuccessful();
        }

        @Test
        @DisplayName("CommandSource WITHOUT cooldown-bypass permission should be blocked")
        void sourceWithoutBypassPermissionShouldBeBlocked() {
            String bypassPerm = "cooldown.bypass";
            TestImperat imperat = TestImperatConfig.builder()
                                          .permissionChecker((src, perm) -> perm == null || src.hasPermission(perm))
                                          .build();

            Command<TestCommandSource> cmd = Command.<TestCommandSource>create(imperat, "cd8")
                                              .defaultExecution((source, context) -> source.reply("cd8 default"))
                                              .pathway(
                                                      CommandPathway.<TestCommandSource>builder()
                                                              .parameters(Argument.requiredText("target"))
                                                              .cooldown(10, TimeUnit.SECONDS, bypassPerm)
                                                              .execute((source, context) -> source.reply(
                                                                      "cd8 target=" + context.getArgument("target")))
                                              )
                                              .build();

            imperat.registerSimpleCommand(cmd);

            // CommandSource does NOT have the bypass permission
            TestCommandSource source = new TestCommandSource(System.out);

            ExecutionResult<TestCommandSource> first = imperat.execute(source, "cd8 player1");
            assertThat(first).isSuccessful();

            ExecutionResult<TestCommandSource> second = imperat.execute(source, "cd8 player1");
            assertThat(second).hasFailed();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Subcommand cooldown isolation
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Subcommand Cooldown")
    class SubcommandCooldown {

        @Test
        @DisplayName("Subcommand cooldown should block repeated subcommand execution")
        void subcommandCooldownShouldBlock() {
            TestImperat imperat = buildImperatWithSubcommandCooldowns();
            TestCommandSource source = new TestCommandSource(System.out);

            ExecutionResult<TestCommandSource> first = imperat.execute(source, "coolsub action hello");
            assertThat(first).isSuccessful();

            ExecutionResult<TestCommandSource> second = imperat.execute(source, "coolsub action hello");
            assertThat(second).hasFailed();
        }

        @Test
        @DisplayName("Root default execution should not be affected by subcommand cooldown")
        void rootDefaultShouldNotBeAffectedBySubcommandCooldown() {
            TestImperat imperat = buildImperatWithSubcommandCooldowns();
            TestCommandSource source = new TestCommandSource(System.out);

            // Execute subcommand (triggers cooldown on subcommand pathway)
            ExecutionResult<TestCommandSource> subResult = imperat.execute(source, "coolsub action hello");
            assertThat(subResult).isSuccessful();

            // Root default should still work — different pathway, no cooldown
            ExecutionResult<TestCommandSource> rootResult = imperat.execute(source, "coolsub");
            assertThat(rootResult).isSuccessful();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  No cooldown — commands without cooldown should never be blocked
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("No Cooldown Configured")
    class NoCooldown {

        @Test
        @DisplayName("Commands without cooldown should allow unlimited rapid execution")
        void commandsWithoutCooldownShouldNotBlock() {
            TestImperat imperat = TestImperatConfig.builder().build();

            Command<TestCommandSource> cmd = Command.<TestCommandSource>create(imperat, "nocd")
                                              .defaultExecution((source, context) -> source.reply("nocd executed"))
                                              .pathway(
                                                      CommandPathway.<TestCommandSource>builder()
                                                              .parameters(Argument.requiredText("name"))
                                                              .execute((source, context) -> source.reply("nocd name=" + context.getArgument("name")))
                                              )
                                              .build();

            imperat.registerSimpleCommand(cmd);
            TestCommandSource source = new TestCommandSource(System.out);

            for (int i = 0; i < 5; i++) {
                ExecutionResult<TestCommandSource> result = imperat.execute(source, "nocd player" + i);
                assertThat(result).isSuccessful();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Cooldown with different arguments — same pathway cooldown regardless
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cooldown With Varying Arguments")
    class CooldownWithVaryingArgs {

        @Test
        @DisplayName("Cooldown should persist even when argument values differ")
        void cooldownShouldPersistWithDifferentArgValues() {
            TestImperat imperat = buildImperatWithCooldownCommand("cd9", 10, TimeUnit.SECONDS, null);
            TestCommandSource source = new TestCommandSource(System.out);

            ExecutionResult<TestCommandSource> first = imperat.execute(source, "cd9 player1");
            assertThat(first).isSuccessful();

            // Same pathway, different argument value — should still be on cooldown
            ExecutionResult<TestCommandSource> second = imperat.execute(source, "cd9 player2");
            assertThat(second).hasFailed();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Multiple pathways — independent cooldowns
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Multiple Pathway Cooldowns")
    class MultiplePathwayCooldowns {

        @Test
        @DisplayName("Different pathways should have independent cooldowns")
        void differentPathwaysShouldHaveIndependentCooldowns() {
            TestImperat imperat = TestImperatConfig.builder().build();

            Command<TestCommandSource> cmd = Command.<TestCommandSource>create(imperat, "multi")
                                              .pathway(
                                                      CommandPathway.<TestCommandSource>builder()
                                                              .cooldown(10, TimeUnit.SECONDS)
                                                              .execute((source, context) -> source.reply("multi default"))
                                              )
                                              .pathway(
                                                      CommandPathway.<TestCommandSource>builder()
                                                              .parameters(Argument.requiredText("target"))
                                                              .cooldown(10, TimeUnit.SECONDS)
                                                              .execute((source, context) -> source.reply(
                                                                      "multi target=" + context.getArgument("target")))
                                              )
                                              .build();

            imperat.registerSimpleCommand(cmd);
            TestCommandSource source = new TestCommandSource(System.out);

            // Execute default pathway
            ExecutionResult<TestCommandSource> defaultResult = imperat.execute(source, "multi");
            assertThat(defaultResult).isSuccessful();

            // Execute target pathway — different pathway, should succeed
            ExecutionResult<TestCommandSource> targetResult = imperat.execute(source, "multi someTarget");
            assertThat(targetResult).isSuccessful();

            // Execute default pathway again — should be on cooldown
            ExecutionResult<TestCommandSource> defaultAgain = imperat.execute(source, "multi");
            assertThat(defaultAgain).hasFailed();

            // Execute target pathway again — should also be on cooldown
            ExecutionResult<TestCommandSource> targetAgain = imperat.execute(source, "multi anotherTarget");
            assertThat(targetAgain).hasFailed();
        }
    }
}



