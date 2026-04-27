package studio.mevera.imperat.tests.basics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies that an embedder using only the programmatic
 * {@code registerSimpleCommand} path never triggers annotation-parser
 * construction. Proves Track 5's separability claim: {@code BaseImperat}
 * can be driven without the annotation layer.
 */
@DisplayName("Programmatic-Only Registration Tests")
final class ProgrammaticRegistrationTest {

    private static Object readAnnotationParser(TestImperat imperat) throws Exception {
        Class<?> base = Class.forName("studio.mevera.imperat.BaseImperat");
        Field f = base.getDeclaredField("annotationParser");
        f.setAccessible(true);
        return f.get(imperat);
    }

    @Test
    @DisplayName("Annotation parser stays null until first annotation method invoked")
    void parserStaysNullForProgrammaticOnly() throws Exception {
        TestImperat imperat = TestImperatConfig.builder().build();

        // Build a command entirely in code: /greet <name>
        AtomicReference<String> seenName = new AtomicReference<>();
        Command<TestCommandSource> greet = Command.create(imperat, "greet")
                                                   .pathway(CommandPathway.<TestCommandSource>builder()
                                                                    .arguments(Argument.requiredText("name"))
                                                                    .execute((source, ctx) -> seenName.set(ctx.getArgument("name"))))
                                                   .build();

        // No annotation methods called yet — parser must remain null.
        assertNull(readAnnotationParser(imperat),
                "Parser must be null before any annotation-related call");

        imperat.registerSimpleCommand(greet);

        // registerSimpleCommand does NOT touch annotations — parser still null.
        assertNull(readAnnotationParser(imperat),
                "registerSimpleCommand must not trigger parser construction");

        // Execute via the programmatic path.
        ExecutionResult<TestCommandSource> result = imperat.execute(imperat.createDummySender(), "greet alice");
        assertFalse(result.hasFailed(), "Execution should succeed");
        assertEquals("alice", seenName.get());

        // Still no parser — execution path is parser-free.
        assertNull(readAnnotationParser(imperat),
                "Command execution must not trigger parser construction");
    }

    @Test
    @DisplayName("Parser materialises lazily on first annotation call")
    void parserMaterialisesOnFirstAnnotationCall() throws Exception {
        TestImperat imperat = TestImperatConfig.builder().build();
        assertNull(readAnnotationParser(imperat));

        imperat.getAnnotationParser();
        assertNotNull(readAnnotationParser(imperat),
                "getAnnotationParser must lazily build the parser");
    }
}
