package studio.mevera.imperat.tests.subcommands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

@DisplayName("Subcommand Tests")
public class SubcommandTest extends BaseImperatTest {
    
    @Test
    @DisplayName("Should execute simple subcommand")
    void testSimpleSubcommand() {
        ExecutionResult<TestSource> result = execute("kit create test");
        assertSuccess(result);
        assertArgument(result, "kit", "test");
        assertArgument(result, "weight", 1); // Default value
    }
    
    @Test
    @DisplayName("Should execute subcommand with optional parameters")
    void testSubcommandWithOptionalParameters() {
        ExecutionResult<TestSource> result = execute("kit create test 5");
        assertSuccess(result);
        assertArgument(result, "kit", "test");
        assertArgument(result, "weight", 5);
    }
    
    @Test
    @DisplayName("Should execute nested subcommands")
    void testNestedSubcommands() {
        ExecutionResult<TestSource> result = execute("test hello world sub1 value");
        assertSuccess(result);
        assertArgument(result, "otherText", "hello");
        assertArgument(result, "otherText2", "world");
        assertArgument(result, "a", "value");
    }
    
    @Test
    @DisplayName("Should execute deeply nested subcommands")
    void testDeeplyNestedSubcommands() {
        ExecutionResult<TestSource> result = execute("test hello world sub1 a1 sub2 b1 sub3 c1");
        assertSuccess(result);
        assertArgument(result, "otherText", "hello");
        assertArgument(result, "otherText2", "world");
        assertArgument(result, "a", "a1");
        assertArgument(result, "b", "b1");
        assertArgument(result, "c", "c1");
    }
    
    @Test
    @DisplayName("Should handle sub4 command tree")
    void testSub4CommandTree() {
        ExecutionResult<TestSource> result = execute("test hello world sub4 a1 sub5 b1 sub6 c1");
        assertSuccess(result);
        assertArgument(result, "otherText", "hello");
        assertArgument(result, "otherText2", "world");
        assertArgument(result, "a", "a1");
        assertArgument(result, "b", "b1");
        assertArgument(result, "c", "c1");
    }
    
    @Test
    @DisplayName("Should handle non-static inner class subcommands")
    void testNonStaticInnerClassSubcommands() {
        ExecutionResult<TestSource> result = execute("root i1");
        assertSuccess(result);
        
        result = execute("root i1 i1.1");
        assertSuccess(result);
        
        result = execute("root i1 i1.1 i1.1.1");
        assertSuccess(result);
        
        result = execute("root i2");
        assertSuccess(result);
        
        result = execute("root i2 i2.1");
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should handle help subcommand")
    void testHelpSubcommand() {
        ExecutionResult<TestSource> result = execute("test help");
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should handle embedded commands")
    void testEmbeddedCommands() {
        ExecutionResult<TestSource> result = execute("embedded testvalue");
        assertSuccess(result);
        assertArgument(result, "value", "testvalue");
    }
    
    @Test
    @DisplayName("Should send proper help message using help subcommand")
    void testGroupHelpSubCommand() {
        var res = execute("group");
        assertSuccess(res);
    }
}