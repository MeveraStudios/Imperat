package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

@DisplayName("Enhanced Argument Parsing Tests")
class EnhancedArgumentParsingTest extends EnhancedBaseImperatTest {
    
    @Test
    @DisplayName("Should parse required arguments with fluent assertions")
    void testRequiredArgumentsWithFluentAssertions() throws ImperatException {
        ExecutionResult<TestSource> result = execute("test hello world");
        
        assertThat(result)
            .isSuccessful()
            .hasArgument("otherText", "hello")
            .hasArgument("otherText2", "world");
    }
    
    @Test
    @DisplayName("Should handle give command with complex argument combinations")
    void testGiveCommandComplexArguments() throws ImperatException {
        ExecutionResult<TestSource> result = execute("give diamond_sword player123 64");
        
        assertThat(result)
            .isSuccessful()
            .hasArgument("item", "diamond_sword")
            .hasArgumentSatisfying("player", player -> {
                Assertions.assertThat(player).isInstanceOf(TestPlayer.class);
                Assertions.assertThat(player.toString()).isEqualTo("player123");
            })
            .hasArgument("amount", 64);
    }
    
    @Test
    @DisplayName("Should handle optional arguments elegantly")
    void testOptionalArgumentsElegantly() throws ImperatException {
        ExecutionResult<TestSource> result = execute("give apple");
        
        assertThat(result)
            .isSuccessful()
            .hasArgument("item", "apple")
            .hasNullArgument("player")
            .hasArgument("amount", 1);
    }
    
    @ParameterizedTest
    @CsvSource({
        "test2 array member mod admin, 3",
        "test2 array owner, 1", 
        "test2 array guest vip premium ultra, 4"
    })
    @DisplayName("Should parse array arguments with dynamic size validation")
    void testArrayArgumentsWithValidation(String commandLine, int expectedSize) throws ImperatException {
        ExecutionResult<TestSource> result = execute(commandLine);
        
        assertThat(result)
            .isSuccessful()
            .hasArgumentSatisfying("myArray", array -> {
                Assertions.assertThat(array)
                    .isInstanceOf(String[].class)
                    .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.array(String[].class))
                    .hasSize(expectedSize);
            });
    }
    
    @Test
    @DisplayName("Should validate greedy arguments capture everything")
    void testGreedyArgumentsValidation() throws ImperatException {
        ExecutionResult<TestSource> result = execute("message target_player This is a very long message with many words");
        
        assertThat(result)
            .isSuccessful()
            .hasArgument("target", "target_player")
            .hasArgument("message", "This is a very long message with many words");
    }
    
    @Test
    @DisplayName("Should demonstrate satisfies methods usage")
    void testSatisfiesMethodsUsage() throws ImperatException {
        ExecutionResult<TestSource> result = execute("give diamond_sword player123 64");
        
        // Using satisfies on the ExecutionResult itself
        assertThat(result)
            .isSuccessful()
            .satisfies(executionResult -> {
                // Custom validation on the entire execution result
                Assertions.assertThat(executionResult.getExecutionContext()).isNotNull();
                Assertions.assertThat(executionResult.getSearch()).isNotNull();
                Assertions.assertThat(executionResult.getError()).isNull();
            })
            .hasArgument("item", "diamond_sword")
            .hasArgumentSatisfying("player", player -> {
                // Custom validation on a specific argument
                Assertions.assertThat(player).isInstanceOf(TestPlayer.class);
                Assertions.assertThat(player.toString()).isEqualTo("player123");
            })
            .hasArgument("amount", 64);
    }
    
    @Test
    @DisplayName("Should demonstrate satisfiesAll for multiple validations")
    void testSatisfiesAllUsage() throws ImperatException {
        ExecutionResult<TestSource> result = execute("ban griefer -s -ip 7d Griefing");
        
        this.assertThat(result)
            .isSuccessful()
            .satisfiesAll(
                // First validation
                executionResult -> {
                    Assertions.assertThat((Object) executionResult.getExecutionContext().getArgument("target")).isEqualTo("griefer");
                },
                // Second validation
                executionResult -> {
                    Assertions.assertThat((Object)executionResult.getExecutionContext().getFlagValue("silent")).isEqualTo(true);
                    Assertions.assertThat((Object)executionResult.getExecutionContext().getFlagValue("ip")).isEqualTo(true);
                },
                // Third validation
                executionResult -> {
                    Assertions.assertThat((Object)executionResult.getExecutionContext().getArgument("duration")).isEqualTo("7d");
                    Assertions.assertThat((Object)executionResult.getExecutionContext().getArgument("reason")).isEqualTo("Griefing");
                }
            );
    }
    
    @Test
    @DisplayName("Should handle collection parameters with fluent validation")
    void testCollectionParametersFluentValidation() throws ImperatException {
        ExecutionResult<TestSource> result = execute("test2 collection hello world amazing test");
        
        assertThat(result)
            .isSuccessful()
            .hasArgumentSatisfying("myCollection", collection -> {
                Assertions.assertThat(collection)
                    .asList()
                    .hasSize(4)
                    .contains("hello", "world", "amazing", "test");
            });
    }
    
    @Test
    @DisplayName("Should handle map parameters with key-value validation")
    void testMapParametersKeyValueValidation() throws ImperatException {
        ExecutionResult<TestSource> result = execute("test2 map name,John age,25 city,Paris");
        
        assertThat(result)
            .isSuccessful()
            .hasArgumentSatisfying("myMap", map -> {
                Assertions.assertThat(map)
                    .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                    .hasSize(3)
                    .containsEntry("name", "John")
                    .containsEntry("age", "25")
                    .containsEntry("city", "Paris");
            });
    }
}
