package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;
import studio.mevera.imperat.tests.commands.MultipleOptionals;
import studio.mevera.imperat.tests.commands.realworld.GiveCmd;
import studio.mevera.imperat.tests.parameters.TestPlayerParamType;

@DisplayName("Enhanced Argument Parsing Tests")
class EnhancedArgumentParsingTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Should parse required arguments with fluent assertions")
    void testRequiredArgumentsWithFluentAssertions() {
        ExecutionResult<TestSource> result = execute("test hello world");

        assertThat(result)
                .isSuccessful()
                .hasArgument("otherText", "hello")
                .hasArgument("otherText2", "world");
    }

    @Test
    @DisplayName("Should handle give command with complex argument combinations")
    void testGiveCommandComplexArguments() throws CommandException {
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
    void testOptionalArgumentsElegantly() throws CommandException {
        ExecutionResult<TestSource> result = execute("give apple");
        assertThat(result)
                .isSuccessful()
                .hasArgument("item", "apple")
                .hasNullArgument("player")
                .hasArgument("amount", 1);
    }

    @Test
    @DisplayName("Should handle optional args suggestion overlapping ONLY when enabled")
    void testOptionalArgsSuggestionsOverlapping() {
        var results = tabComplete(GiveCmd.class, (cfg) -> {
            cfg.registerParamType(TestPlayer.class, new TestPlayerParamType());
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "give apple ");

        Assertions.assertThatList(results)
                .containsExactly("MQZEN", "MOHAMED", "1", "2", "3");
    }

    @Test
    @DisplayName("Should handle optional args suggestion overlapping ONLY when enabled #2")
    void testOptionalArgsSuggestionsOverlapping2() {
        var results = tabComplete(GiveCmd.class, (cfg) -> {
            cfg.registerParamType(TestPlayer.class, new TestPlayerParamType());
            cfg.setOptionalParameterSuggestionOverlap(false);
        }, "give apple ");

        Assertions.assertThatList(results)
                .containsExactly("MQZEN", "MOHAMED");
    }

    @Test
    @DisplayName("Should handle optional args suggestion overlapping ONLY when enabled #3")
    void testOptionalArgsSuggestionsOverlapping3() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "multopts ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("hi", "7.5");
    }

    @Test
    @DisplayName("Should handle optional args suggestion overlapping ONLY when enabled #4")
    void testOptionalArgsSuggestionsOverlapping4() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "multopts opt1 ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("stop-point", "7.5");
    }

    @Test
    @DisplayName("Should NOT overlap when consecutive optionals have SAME type")
    void testOptionalOverlappingSameType() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "sametype ");

        Assertions.assertThatList(results)
                .containsExactly("option1", "option2");
    }

    @Test
    @DisplayName("Should overlap through multiple consecutive optionals with different types")
    void testMultipleConsecutiveOptionals() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "chain ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("text1", "text2", "100", "200", "3.14", "2.71");
    }

    @Test
    @DisplayName("Should stop at required parameter and include it IF THEY HAVE DIFFERENT TYPES")
    void testStopAtRequired() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "optreq ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("opt1-value");
    }

    @Test
    @DisplayName("Should NOT show Optional2 after required even with overlap enabled")
    void testNoOverlapBeyondRequired() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "mixedcmd ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("first", "second", "999");
    }

    @Test
    @DisplayName("Should handle overlap at deeper position in command")
    void testOverlapAtDeeperPosition() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "deep required1 ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("opt1-a", "opt1-b", "7", "8");
    }

    @Test
    @DisplayName("Should NOT overlap when disabled even with different types")
    void testOverlapDisabled() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(false);
        }, "chain ");

        Assertions.assertThatList(results)
                .containsExactly("text1", "text2");
    }

    @Test
    @DisplayName("Should handle single optional followed by required")
    void testSingleOptionalWithRequired() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "simple ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("Player1", "Player2", "50", "100", "200");
    }

    @Test
    @DisplayName("Should NOT duplicate suggestions when tree has multiple paths")
    void testNoDuplicationInMultiplePaths() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "branch item1 ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("path1", "path2", "10", "20")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should handle all optionals of different types")
    void testAllOptionalsScenario() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "allopts ");

        Assertions.assertThatList(results)
                .containsExactlyInAnyOrder("str1", "str2", "1", "2", "1.5", "2.5", "true", "false");
    }

    @Test
    @DisplayName("Should only show immediate optional when at that position without overlap")
    void testNoOverlapAtSpecificPosition() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(false);
        }, "multopts opt1 ");

        Assertions.assertThatList(results)
                .containsExactly("7.5");
    }

    @Test
    @DisplayName("Should correctly handle empty optional paths")
    void testEmptyOptionalPaths() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "empty val1 val2 ");

        Assertions.assertThatList(results)
                .isNotEmpty()
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should handle overlap correctly after consuming first optional")
    void testOverlapAfterFirstOptional() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "chain text1 ");

        Assertions.assertThatList(results)
                .containsExactly("100", "200", "3.14", "2.71", "final");
    }

    @Test
    @DisplayName("Should not overlap same types even after consuming arguments")
    void testNoOverlapSameTypeAfterConsuming() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "sametype option1 ");

        Assertions.assertThatList(results)
                .containsExactly("option3", "option4");
    }

    @Test
    @DisplayName("Should handle deep position with overlap disabled")
    void testDeepPositionNoOverlap() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(false);
        }, "deep req1 ");

        Assertions.assertThatList(results)
                .containsExactly("opt1-a", "opt1-b");
    }

    @Test
    @DisplayName("Should handle deep position with overlap enabled")
    void testDeepPositionWithOverlap() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "deep req1 ");

        Assertions.assertThatList(results)
                .containsExactly("opt1-a", "opt1-b", "7", "8");
    }

    @Test
    @DisplayName("Should handle all optionals consumed scenario")
    void testAllOptionalsConsumed() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "allopts str1 1 1.5 ");

        Assertions.assertThatList(results)
                .containsExactly("true", "false");
    }

    @Test
    @DisplayName("Should show nothing when all parameters consumed")
    void testAllParametersConsumed() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "chain text1 100 3.14 final ");

        Assertions.assertThatList(results)
                .isEmpty();
    }

    @Test
    @DisplayName("Should handle branch command with overlap at second position")
    void testBranchSecondPosition() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "branch item1 ");

        Assertions.assertThatList(results)
                .containsExactly("path1", "path2", "10", "20");
    }

    @Test
    @DisplayName("Should handle simple command with consumed optional")
    void testSimpleWithConsumedOptional() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(false);
        }, "simple Player1 ");

        Assertions.assertThatList(results)
                .containsExactly("50", "100", "200");
    }

    @Test
    @DisplayName("Should handle empty command at third position")
    void testEmptyThirdPosition() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "empty val1 val2 ");

        Assertions.assertThatList(results)
                .containsExactly("opt1", "5");
    }

    @Test
    @DisplayName("Should handle chain with two optionals consumed")
    void testChainTwoOptionalsConsumed() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "chain text1 100 ");

        Assertions.assertThatList(results)
                .containsExactly("3.14", "2.71", "final");
    }

    @Test
    @DisplayName("Should handle multopts at base position with overlap")
    void testMultoptsBaseWithOverlap() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "multopts ");

        Assertions.assertThatList(results)
                .containsExactly("hi", "7.5");
    }

    @Test
    @DisplayName("Resolves the current optional suggestions only when overlap=false AND next required arg is of different type.")
    void testEmptyRequiredStopPoint() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(false);
        }, "empty val1 val2 ");

        Assertions.assertThatList(results)
                .containsExactly("opt1");
    }

    @Test
    @DisplayName("Should handle multopts with overlap disabled")
    void testMultoptsNoOverlap() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(false);
        }, "multopts ");

        Assertions.assertThatList(results)
                .containsExactly("hi");
    }

    @Test
    @DisplayName("Should show only required after all optionals in allopts")
    void testAllOptsWithRequired() {
        var results = tabComplete(MultipleOptionals.class, (cfg) -> {
            cfg.setOptionalParameterSuggestionOverlap(true);
        }, "allopts str1 1 1.5 true ");

        Assertions.assertThatList(results)
                .isEmpty();
    }


    @ParameterizedTest
    @CsvSource({
            "test2 array member mod admin, 3",
            "test2 array owner, 1",
            "test2 array guest vip premium ultra, 4"
    })
    @DisplayName("Should parse array arguments with dynamic size validation")
    void testArrayArgumentsWithValidation(String commandLine, int expectedSize) throws CommandException {
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
    void testGreedyArgumentsValidation() throws CommandException {
        ExecutionResult<TestSource> result = execute("message target_player This is a very long message with many words");

        assertThat(result)
                .isSuccessful()
                .hasArgument("target", "target_player")
                .hasArgument("message", "This is a very long message with many words");
    }

    @Test
    @DisplayName("Should demonstrate satisfies methods usage")
    void testSatisfiesMethodsUsage() throws CommandException {
        ExecutionResult<TestSource> result = execute("give diamond_sword player123 64");

        assertThat(result)
                .isSuccessful()
                .satisfies(executionResult -> {
                    Assertions.assertThat(executionResult.getExecutionContext()).isNotNull();
                    Assertions.assertThat(executionResult.getSearch()).isNotNull();
                    Assertions.assertThat(executionResult.getError()).isNull();
                })
                .hasArgument("item", "diamond_sword")
                .hasArgumentSatisfying("player", player -> {
                    Assertions.assertThat(player).isInstanceOf(TestPlayer.class);
                    Assertions.assertThat(player.toString()).isEqualTo("player123");
                })
                .hasArgument("amount", 64);
    }


    @Test
    @DisplayName("Should handle collection parameters with fluent validation")
    void testCollectionParametersFluentValidation() throws CommandException {
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
    void testMapParametersKeyValueValidation() throws CommandException {
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

    @Test
    @DisplayName("Should handle proper TRUE-FLAG tab-completion for its value")
    void testTrueFlagTabCompletion() {
        var res = tabComplete("bal -c ");
        Assertions.assertThat(res).containsExactly("gold", "silver");
    }
}
