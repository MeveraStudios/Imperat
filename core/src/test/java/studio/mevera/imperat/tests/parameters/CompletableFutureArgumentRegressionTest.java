package studio.mevera.imperat.tests.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentTypes;
import studio.mevera.imperat.command.arguments.type.GreedyArgumentType;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.util.TypeWrap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Regression coverage for the {@code CompletableFutureArgument} multi-arity
 * drain bug.
 *
 * <p><b>What was broken:</b> the legacy implementation drained exactly one
 * token from the cursor (via {@code cursor.nextOrNull()}) for every non-greedy
 * inner type, regardless of the inner type's actual arity. As a result a
 * {@code CompletableFuture<T>} where {@code T} was a fixed multi-token type
 * (e.g. a 3-token coordinate triple built on {@code SimpleArgumentType(N)})
 * would receive only the first token, leave the remaining tokens on the
 * stream, and silently steal them from any downstream argument.</p>
 *
 * <p><b>What this test exercises (each is an end-to-end command run):</b>
 * <ol>
 *   <li><b>Single-token inner</b> — baseline: {@code CompletableFuture<Integer>}
 *       still resolves the same way it did pre-fix.</li>
 *   <li><b>Multi-token inner</b> — the regression: {@code CompletableFuture<TriCoord>}
 *       must consume exactly three tokens and produce the joined value.</li>
 *   <li><b>Multi-token inner followed by a downstream argument</b> — proves
 *       the future does not steal tokens from the next argument; the legacy
 *       bug would have left the downstream arg with the second token and
 *       failed to parse the trailing one.</li>
 *   <li><b>Greedy inner</b> — verifies the long-standing greedy path
 *       (collect rest-of-line) still works.</li>
 * </ol>
 *
 * <p>"Insufficient tokens" / "empty cursor" cases are not directly testable
 * end-to-end because the tree's pre-allocation logic
 * ({@code getNumberOfParametersToConsume}) refuses to admit a candidate that
 * lacks the required token count — those failure modes are caught one layer
 * up before {@code CompletableFutureArgument#parse} is ever invoked.</p>
 */
@DisplayName("CompletableFutureArgument Multi-Arity Regression")
final class CompletableFutureArgumentRegressionTest {

    /** Awaits the captured future with a short timeout to keep the suite snappy. */
    private static <T> T await(AtomicReference<CompletableFuture<T>> ref) throws Exception {
        CompletableFuture<T> fut = ref.get();
        assertNotNull(fut, "Command did not capture a future");
        return fut.get(5, TimeUnit.SECONDS);
    }

    private static TestImperat freshImperat() {
        return TestImperatConfig.builder()
                       .argType(TriCoord.class, new TriCoordArgumentType())
                       .build();
    }

    @Test
    @DisplayName("Single-token inner: CompletableFuture<Integer> baseline still resolves")
    void singleTokenInner() throws Exception {
        TestImperat imperat = freshImperat();
        AtomicReference<CompletableFuture<Integer>> captured = new AtomicReference<>();

        Command<TestCommandSource> cmd = Command.create(imperat, "n")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.<TestCommandSource, CompletableFuture<Integer>>required(
                                                                          "n",
                                                                          ArgumentTypes.future(
                                                                                  new TypeWrap<>() {
                                                                                  },
                                                                                  ArgumentTypes.numeric(Integer.class)
                                                                          )
                                                                  ).build())
                                                                  .execute((source, ctx) -> captured.set(ctx.getArgument("n"))))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result = imperat.execute(imperat.createDummySender(), "n 42");
        assertFalse(result.hasFailed());
        assertEquals(42, await(captured));
    }

    @Test
    @DisplayName("Multi-token inner: CompletableFuture<TriCoord> consumes all three tokens")
    void multiTokenInnerConsumesAllTokens() throws Exception {
        TestImperat imperat = freshImperat();
        AtomicReference<CompletableFuture<TriCoord>> captured = new AtomicReference<>();

        Command<TestCommandSource> cmd = Command.create(imperat, "tp")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.<TestCommandSource, CompletableFuture<TriCoord>>required(
                                                                          "where",
                                                                          ArgumentTypes.future(
                                                                                  new TypeWrap<>() {
                                                                                  },
                                                                                  new TriCoordArgumentType()
                                                                          )
                                                                  ).build())
                                                                  .execute((source, ctx) -> captured.set(ctx.getArgument("where"))))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result = imperat.execute(imperat.createDummySender(), "tp 10 20 30");
        assertFalse(result.hasFailed(), "Multi-token CF parse must succeed end-to-end");

        TriCoord coord = await(captured);
        assertEquals(10.0, coord.x, "x must be the first drained token");
        assertEquals(20.0, coord.y, "y must be the second drained token (was lost pre-fix)");
        assertEquals(30.0, coord.z, "z must be the third drained token (was lost pre-fix)");
    }

    @Test
    @DisplayName("Multi-token inner followed by another arg: future must NOT steal downstream tokens")
    void multiTokenInnerLeavesDownstreamUntouched() throws Exception {
        TestImperat imperat = freshImperat();
        AtomicReference<CompletableFuture<TriCoord>> capturedCoord = new AtomicReference<>();
        AtomicReference<String> capturedLabel = new AtomicReference<>();

        // /tp <where=tricoord(3)> <label=string>
        // Pre-fix: future drains 1 token → leaves 'y' as the next arg → 'z' becomes
        // an unbindable trailing token → parse fails OR label captures the wrong value.
        Command<TestCommandSource> cmd = Command.create(imperat, "tp")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(
                                                                          Argument.<TestCommandSource, CompletableFuture<TriCoord>>required(
                                                                                  "where",
                                                                                  ArgumentTypes.future(
                                                                                          new TypeWrap<>() {
                                                                                          },
                                                                                          new TriCoordArgumentType()
                                                                                  )
                                                                          ).build(),
                                                                          Argument.<TestCommandSource>requiredText("label").build()
                                                                  )
                                                                  .execute((source, ctx) -> {
                                                                      capturedCoord.set(ctx.getArgument("where"));
                                                                      capturedLabel.set(ctx.getArgument("label"));
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result = imperat.execute(imperat.createDummySender(), "tp 10 20 30 spawnpoint");
        assertFalse(result.hasFailed(), "Future must release its 3 tokens cleanly so the downstream arg parses");

        TriCoord coord = await(capturedCoord);
        assertEquals(10.0, coord.x);
        assertEquals(20.0, coord.y);
        assertEquals(30.0, coord.z);
        assertEquals("spawnpoint", capturedLabel.get(),
                "Downstream arg must receive its own token, not one stolen by the future");
    }

    @Test
    @DisplayName("Required arg before CF: future starts at correct cursor position")
    void multiTokenInnerAfterUpstreamArg() throws Exception {
        TestImperat imperat = freshImperat();
        AtomicReference<String> capturedTag = new AtomicReference<>();
        AtomicReference<CompletableFuture<TriCoord>> capturedCoord = new AtomicReference<>();

        // /tag <name=string> <where=CF<TriCoord>>
        // Pre-fix: future drains 1 token → only 'x' goes into the future, 'y' and 'z'
        // are left as trailing input → tree fails to fully consume → parse fails.
        Command<TestCommandSource> cmd = Command.create(imperat, "tag")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(
                                                                          Argument.<TestCommandSource>requiredText("name").build(),
                                                                          Argument.<TestCommandSource, CompletableFuture<TriCoord>>required(
                                                                                  "where",
                                                                                  ArgumentTypes.future(
                                                                                          new TypeWrap<>() {
                                                                                          },
                                                                                          new TriCoordArgumentType()
                                                                                  )
                                                                          ).build()
                                                                  )
                                                                  .execute((source, ctx) -> {
                                                                      capturedTag.set(ctx.getArgument("name"));
                                                                      capturedCoord.set(ctx.getArgument("where"));
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result = imperat.execute(imperat.createDummySender(), "tag spawn 10 20 30");
        assertFalse(result.hasFailed(), "Upstream arg must release exactly its token, future must drain the next 3");

        assertEquals("spawn", capturedTag.get(), "Upstream arg must receive its single token");
        TriCoord coord = await(capturedCoord);
        assertEquals(10.0, coord.x);
        assertEquals(20.0, coord.y);
        assertEquals(30.0, coord.z);
    }

    @Test
    @DisplayName("Sandwich: arg → CF<TriCoord> → arg; future leaves both neighbors intact")
    void multiTokenInnerSandwiched() throws Exception {
        TestImperat imperat = freshImperat();
        AtomicReference<String> capturedTag = new AtomicReference<>();
        AtomicReference<CompletableFuture<TriCoord>> capturedCoord = new AtomicReference<>();
        AtomicReference<Integer> capturedRadius = new AtomicReference<>();

        // /place <tag=string> <where=CF<TriCoord>> <radius=int>
        // Tests both directions at once: the future must start where the
        // upstream arg ended AND release cleanly so the downstream arg parses.
        Command<TestCommandSource> cmd = Command.create(imperat, "place")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(
                                                                          Argument.<TestCommandSource>requiredText("tag").build(),
                                                                          Argument.<TestCommandSource, CompletableFuture<TriCoord>>required(
                                                                                  "where",
                                                                                  ArgumentTypes.future(
                                                                                          new TypeWrap<>() {
                                                                                          },
                                                                                          new TriCoordArgumentType()
                                                                                  )
                                                                          ).build(),
                                                                          Argument.<TestCommandSource, Integer>required("radius",
                                                                                  ArgumentTypes.numeric(Integer.class)).build()
                                                                  )
                                                                  .execute((source, ctx) -> {
                                                                      capturedTag.set(ctx.getArgument("tag"));
                                                                      capturedCoord.set(ctx.getArgument("where"));
                                                                      capturedRadius.set(ctx.getArgument("radius"));
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result = imperat.execute(imperat.createDummySender(), "place beacon 100 64 200 5");
        assertFalse(result.hasFailed(), "Sandwiched future must drain exactly 3 middle tokens");

        assertEquals("beacon", capturedTag.get(), "Upstream arg gets the first token");
        TriCoord coord = await(capturedCoord);
        assertEquals(100.0, coord.x, "Future drains middle token 1");
        assertEquals(64.0, coord.y, "Future drains middle token 2 (was lost pre-fix)");
        assertEquals(200.0, coord.z, "Future drains middle token 3 (was stolen by downstream pre-fix)");
        assertEquals(5, capturedRadius.get(), "Downstream arg receives the trailing token, not '64' or '200'");
    }

    @Test
    @DisplayName("Single-token CF sandwiched: arg → CF<Integer> → arg")
    void singleTokenInnerSandwiched() throws Exception {
        TestImperat imperat = freshImperat();
        AtomicReference<String> capturedHead = new AtomicReference<>();
        AtomicReference<CompletableFuture<Integer>> capturedMid = new AtomicReference<>();
        AtomicReference<String> capturedTail = new AtomicReference<>();

        Command<TestCommandSource> cmd = Command.create(imperat, "wrap")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(
                                                                          Argument.<TestCommandSource>requiredText("head").build(),
                                                                          Argument.<TestCommandSource, CompletableFuture<Integer>>required(
                                                                                  "n",
                                                                                  ArgumentTypes.future(
                                                                                          new TypeWrap<>() {
                                                                                          },
                                                                                          ArgumentTypes.numeric(Integer.class)
                                                                                  )
                                                                          ).build(),
                                                                          Argument.<TestCommandSource>requiredText("tail").build()
                                                                  )
                                                                  .execute((source, ctx) -> {
                                                                      capturedHead.set(ctx.getArgument("head"));
                                                                      capturedMid.set(ctx.getArgument("n"));
                                                                      capturedTail.set(ctx.getArgument("tail"));
                                                                  }))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result = imperat.execute(imperat.createDummySender(), "wrap left 7 right");
        assertFalse(result.hasFailed());

        assertEquals("left", capturedHead.get());
        assertEquals(7, await(capturedMid));
        assertEquals("right", capturedTail.get());
    }

    @Test
    @DisplayName("Greedy inner: CompletableFuture<String> with @Greedy still consumes rest of line")
    void greedyInnerConsumesRestOfLine() throws Exception {
        TestImperat imperat = freshImperat();
        AtomicReference<CompletableFuture<String>> captured = new AtomicReference<>();

        Command<TestCommandSource> cmd = Command.create(imperat, "say")
                                                 .pathway(CommandPathway.<TestCommandSource>builder()
                                                                  .arguments(Argument.<TestCommandSource, CompletableFuture<String>>required(
                                                                          "msg",
                                                                          ArgumentTypes.future(
                                                                                  new TypeWrap<>() {
                                                                                  },
                                                                                  new GreedyStringArgumentType()
                                                                          )
                                                                  ).build())
                                                                  .execute((source, ctx) -> captured.set(ctx.getArgument("msg"))))
                                                 .build();
        imperat.registerSimpleCommand(cmd);

        ExecutionResult<TestCommandSource> result = imperat.execute(imperat.createDummySender(), "say hello brave new world");
        assertFalse(result.hasFailed());
        assertEquals("hello brave new world", await(captured));
    }

    /** Local greedy fixture so this test is self-contained. */
    private static final class GreedyStringArgumentType extends GreedyArgumentType<TestCommandSource, String> {

        @Override
        public String parse(@org.jetbrains.annotations.NotNull studio.mevera.imperat.context.CommandContext<TestCommandSource> context,
                @org.jetbrains.annotations.NotNull Argument<TestCommandSource> argument,
                @org.jetbrains.annotations.NotNull String input) {
            return input;
        }
    }
}
