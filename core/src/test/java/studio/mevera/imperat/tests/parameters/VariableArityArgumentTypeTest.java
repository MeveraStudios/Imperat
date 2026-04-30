package studio.mevera.imperat.tests.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies the Tier-3 (variable-arity custom {@code ArgumentType}) parser
 * dispatch path in {@code Node.parseArgument}. The fixture argument type
 * extends {@code ArgumentType} directly (not {@code SimpleArgumentType},
 * not {@code GreedyArgumentType}) and consumes tokens until a sentinel.
 */
@DisplayName("Variable-Arity (Tier 3) ArgumentType Tests")
final class VariableArityArgumentTypeTest {

    private static TestImperat newImperat() {
        return TestImperatConfig.builder().build();
    }

    private static Command<TestCommandSource> registerVarargsCommand(
            TestImperat imperat,
            AtomicReference<List<String>> capturedItems,
            AtomicReference<String> capturedSuffix
    ) {
        Command<TestCommandSource> cmd = Command.create(imperat, "collect")
                .pathway(CommandPathway.<TestCommandSource>builder()
                        .arguments(
                                Argument.<TestCommandSource, List<String>>required("items", new VariableArityListArgumentType()).build(),
                                Argument.<TestCommandSource>requiredText("suffix").build()
                        )
                        .execute((source, ctx) -> {
                            capturedItems.set(ctx.getArgument("items"));
                            capturedSuffix.set(ctx.getArgument("suffix"));
                        }))
                .build();
        imperat.registerSimpleCommand(cmd);
        return cmd;
    }

    @Test
    @DisplayName("Tier-3 type consumes prefix until sentinel and leaves suffix for next arg")
    void variableArityConsumesUntilSentinel() {
        TestImperat imperat = newImperat();
        AtomicReference<List<String>> items = new AtomicReference<>();
        AtomicReference<String> suffix = new AtomicReference<>();
        registerVarargsCommand(imperat, items, suffix);

        // "alpha beta gamma" eaten by Tier-3, "stop" hits sentinel,
        // remaining suffix arg gets "stop" token.
        ExecutionResult<TestCommandSource> result = imperat.execute(
                imperat.createDummySender(),
                "collect alpha beta gamma stop");

        assertFalse(result.hasFailed(), "Execution should succeed");
        assertNotNull(items.get());
        assertEquals(List.of("alpha", "beta", "gamma"), items.get(),
                "Tier-3 type must consume tokens up to (but not including) the sentinel");
        assertEquals("stop", suffix.get(),
                "Suffix arg must receive the sentinel token left by Tier-3");
    }

    @Test
    @DisplayName("Tier-3 type can consume zero tokens and leave entire input for suffix")
    void variableArityConsumesNothingWhenSentinelIsFirst() {
        TestImperat imperat = newImperat();
        AtomicReference<List<String>> items = new AtomicReference<>();
        AtomicReference<String> suffix = new AtomicReference<>();
        registerVarargsCommand(imperat, items, suffix);

        ExecutionResult<TestCommandSource> result = imperat.execute(
                imperat.createDummySender(),
                "collect stop");

        assertFalse(result.hasFailed());
        assertEquals(List.of(), items.get(),
                "Tier-3 type must consume nothing when sentinel is the first token");
        assertEquals("stop", suffix.get());
    }

    @Test
    @DisplayName("Tier-3 type's cursor.position drives rollback even with single token consumed")
    void variableArityConsumesOneToken() {
        TestImperat imperat = newImperat();
        AtomicReference<List<String>> items = new AtomicReference<>();
        AtomicReference<String> suffix = new AtomicReference<>();
        registerVarargsCommand(imperat, items, suffix);

        ExecutionResult<TestCommandSource> result = imperat.execute(
                imperat.createDummySender(),
                "collect onlyone stop");

        assertFalse(result.hasFailed());
        assertEquals(List.of("onlyone"), items.get());
        assertEquals("stop", suffix.get());
    }
}
