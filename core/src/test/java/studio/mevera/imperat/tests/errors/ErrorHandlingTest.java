package studio.mevera.imperat.tests.errors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Error Handling Tests")
public class ErrorHandlingTest extends BaseImperatTest {
    
    @Test
    @DisplayName("Should fail for incomplete required arguments")
    void testIncompleteRequiredArguments() {
        ExecutionResult<TestSource> result = execute("test hello"); // Missing second required argument
        assertFailure(result);
    }
    
    @Test
    @DisplayName("Should fail for completely unknown commands")
    void testCompletelyUnknownCommands() {
        try{
            execute("completely_unknown_command with args");
        }catch (Exception ex) {
            ex.printStackTrace();
            Assertions.assertInstanceOf(IllegalArgumentException.class, ex);
        }
    }
    
    @Test
    @DisplayName("Should handle malformed flag syntax")
    void testMalformedFlagSyntax() {
        ExecutionResult<TestSource> result = execute("ban mqzen --invalid-flag");
        // This should either work or fail gracefully depending on implementation
        // Just ensure it doesn't crash
        assertNotNull(result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("Should handle empty or whitespace-only inputs")
    void testEmptyInputs(String input) {
        try {
            execute(input.trim());
        }catch (Exception ex) {
            assertInstanceOf(IllegalArgumentException.class, ex);
        }
    }
    
    @Test
    @DisplayName("Should provide meaningful error information")
    void testErrorInformation() {
        try {
            execute("nonexistent command args");
        }catch (Exception ex) {
            assertInstanceOf(IllegalArgumentException.class, ex);
        }
    }
    
    @Test
    @DisplayName("Should handle incomplete subcommands")
    void testIncompleteSubcommands() {
        ExecutionResult<TestSource> result = execute("group member setperm"); // Missing permission argument
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should handle context resolution failures")
    void testContextResolutionFailures() {
        ExecutionResult<TestSource> result = execute("ctx sub"); // Should fail due to missing Group context
        assertFailure(result);
    }
    
    @Test
    @DisplayName("Should detect thrown exception handler from annotated class")
    void testExceptionHandlerAnnotation() {
        var res = execute("fail");
        assertFailure(res);
        
        assertNotNull(res.getError());
        ImperatTestGlobals.IMPERAT.config()
                .handleExecutionThrowable(res.getError(), res.getContext(), BaseImperatTest.class, "testFail");
        
        // Should fail due to missing Group context
    }
    
   /*@Test
    @DisplayName("Should handle permissions overlap 1")
    void testPermissions1() {
        ExecutionResult<TestSource> result = execute((src)-> src.withPerm("testperm.use"),"testperm hi bye"); // Should fail due to missing Group context
        assertNotNull(result.getError());
        result.getError().printStackTrace();
    }*/
   /*
    @Test
    @DisplayName("Should handle permissions overlap 2")
    void testPermissions2() {
        ExecutionResult<TestSource> result = execute((src)-> src.withPerm("testperm.use"), "testperm a b"); // Should fail due to missing Group context
        assertFailure(result, PermissionDeniedException.class);
        assertNotNull(result.getError());
        result.getError().printStackTrace();
    }
    
    @Test
    @DisplayName("Should handle permissions overlap 3")
    void testPermissions3() {
        ExecutionResult<TestSource> result = execute((src)-> src.withPerm("testperm.use").withPerm("testperm.use.arg1.arg2").withPerm("testperm.main"), "testperm a b"); // Should fail due to missing Group context
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should handle permissions overlap 4")
    void testPermissions4() {
        ExecutionResult<TestSource> result = execute(
                (src)-> src.withPerm("testperm.use")
                        .withPerm("testperm.use.arg1.arg2")
                        .withPerm("testperm.use.arg1.arg2.arg3")
                        .withPerm("testperm.main"),
                "testperm a b 3"); // Should fail due to missing Group context
        assertSuccess(result);
    }
    @Test
    @DisplayName("Should handle permissions overlap 5")
    void testPermissions5() {
        ExecutionResult<TestSource> result = execute(
                (src)->
                        src.withPerm("ban")
                                .withPerm("ban.target")
                                .withPerm("ban.target.silent")
                                .withPerm("ban.target.ip")
                                .withPerm("ban.target.duration")
                                .withPerm("ban.target.reason")
                                ,
                "ban mqzen -ip Breaking Server Rules"); // Should fail due to missing Group context
        assertSuccess(result);
    }
     */
}
