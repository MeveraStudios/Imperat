package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.tests.commands.realworld.greedy.LimitedGreedy1Cmd;
import studio.mevera.imperat.tests.commands.realworld.greedy.LimitedGreedy3Cmd;
import studio.mevera.imperat.tests.commands.realworld.greedy.LimitedGreedyWithTrailingArgCmd;
import studio.mevera.imperat.tests.commands.realworld.greedy.LimitedGreedyWithTypedTrailingArgCmd;
import studio.mevera.imperat.tests.commands.realworld.greedy.UnlimitedGreedyCmd;

/**
 * Enhanced tests for the greedy {@code limit} feature on {@code @Greedy(limit = N)}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Unlimited greedy — {@code limit = -1} (default), consumes all remaining tokens</li>
 *   <li>Limit = 1 — behaves like a single-token argument</li>
 *   <li>Limit = 3 — consumes exactly up to 3 tokens</li>
 *   <li>Limit = 3 with fewer tokens supplied — partial consumption</li>
 *   <li>Limited greedy followed by a required trailing argument</li>
 * </ul>
 */
@DisplayName("Greedy limit feature tests")
class EnhancedGreedyLimitTest extends EnhancedBaseImperatTest {

    // ────────────────────────────────────────────────────────────────────────
    // Unlimited greedy (@Greedy / @Greedy(limit = -1))
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Unlimited greedy (limit = -1)")
    class UnlimitedGreedy {

        @Test
        @DisplayName("Captures a single token")
        void singleToken() {
            assertThat(execute(UnlimitedGreedyCmd.class, cfg -> {
            }, "unlimited hello"))
                    .isSuccessful()
                    .hasArgument("text", "hello");
        }

        @Test
        @DisplayName("Captures multiple tokens as one string")
        void multipleTokens() {
            assertThat(execute(UnlimitedGreedyCmd.class, cfg -> {
            }, "unlimited hello world foo bar"))
                    .isSuccessful()
                    .hasArgument("text", "hello world foo bar");
        }

        @Test
        @DisplayName("Captures a long sentence with many words")
        void longSentence() {
            assertThat(execute(UnlimitedGreedyCmd.class, cfg -> {
            }, "unlimited the quick brown fox jumps over the lazy dog"))
                    .isSuccessful()
                    .hasArgument("text", "the quick brown fox jumps over the lazy dog");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Limit = 1
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Greedy with limit = 1")
    class GreedyLimit1 {

        @Test
        @DisplayName("Captures exactly one token when one supplied")
        void exactlyOneToken() {
            assertThat(execute(LimitedGreedy1Cmd.class, cfg -> {
            }, "limited1 hello"))
                    .isSuccessful()
                    .hasArgument("word", "hello");
        }

        @Test
        @DisplayName("Captures only the first token when multiple supplied")
        void stopsAfterOneToken() {
            assertThat(execute(LimitedGreedy1Cmd.class, cfg -> {
            }, "limited1 hello world extra"))
                    .isSuccessful()
                    .hasArgumentSatisfying("word", value ->
                                                           org.assertj.core.api.Assertions.assertThat(value)
                                                                   .asString()
                                                                   .doesNotContain(" ")
                                                                   .isEqualTo("hello")
                    );
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Limit = 3
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Greedy with limit = 3")
    class GreedyLimit3 {

        @Test
        @DisplayName("Captures exactly 3 tokens when exactly 3 supplied")
        void exactlyThreeTokens() {
            assertThat(execute(LimitedGreedy3Cmd.class, cfg -> {
            }, "limited3 one two three"))
                    .isSuccessful()
                    .hasArgument("phrase", "one two three");
        }

        @Test
        @DisplayName("Captures only 3 tokens when more than 3 supplied")
        void stopsAtThreeTokens() {
            assertThat(execute(LimitedGreedy3Cmd.class, cfg -> {
            }, "limited3 one two three four five"))
                    .isSuccessful()
                    .hasArgumentSatisfying("phrase", value -> {
                        String phrase = (String) value;
                        String[] tokens = phrase.split(" ");
                        org.assertj.core.api.Assertions.assertThat(tokens)
                                .as("Should capture exactly 3 tokens")
                                .hasSize(3)
                                .containsExactly("one", "two", "three");
                    });
        }

        @Test
        @DisplayName("Captures fewer than 3 tokens when only 2 supplied")
        void fewerThanLimitTokens() {
            assertThat(execute(LimitedGreedy3Cmd.class, cfg -> {
            }, "limited3 one two"))
                    .isSuccessful()
                    .hasArgumentSatisfying("phrase", value -> {
                        String phrase = (String) value;
                        String[] tokens = phrase.split(" ");
                        org.assertj.core.api.Assertions.assertThat(tokens)
                                .as("Should capture only the 2 supplied tokens")
                                .hasSize(2)
                                .containsExactly("one", "two");
                    });
        }

        @Test
        @DisplayName("Captures a single token when only 1 supplied")
        void oneTokenWithLimit3() {
            assertThat(execute(LimitedGreedy3Cmd.class, cfg -> {
            }, "limited3 only"))
                    .isSuccessful()
                    .hasArgument("phrase", "only");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Limited greedy followed by a trailing required argument
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Limited greedy followed by a trailing required argument")
    class LimitedGreedyWithTrailingArg {

        @Test
        @DisplayName("Limited greedy (limit=2) leaves the last token for the trailing arg")
        void trailingArgIsPopulated() {
            // target="player1", prefix captures "hello world" (2 tokens), suffix="end"
            assertThat(execute(LimitedGreedyWithTrailingArgCmd.class, cfg -> {
            }, "mixed player1 hello world end"))
                    .isSuccessful()
                    .hasArgument("target", "player1")
                    .hasArgument("prefix", "hello world")
                    .hasArgument("suffix", "end");
        }

        @Test
        @DisplayName("Limited greedy (limit=2) with only 1 token consumed leaves trailing arg intact")
        void singleTokenPrefixWithTrailingArg() {
            // target="player1", prefix captures "hello" (1 token), suffix="end"
            assertThat(execute(LimitedGreedyWithTrailingArgCmd.class, cfg -> {
            }, "mixed player1 hello end"))
                    .isSuccessful()
                    .hasArgument("target", "player1")
                    .hasArgument("suffix", "end");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Limited greedy with a typed (non-String) trailing argument
    // e.g. /broadcast <message...(limit=3)> <repeat:int>
    // Greedy yields when the next token matches the next param's type.
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Limited greedy with typed trailing arg (type-discriminated yielding)")
    class LimitedGreedyWithTypedTrailing {

        @Test
        @DisplayName("Yields early when next token matches int type: 'hello world 5' → message='hello world', repeat=5")
        void yieldsToIntWhenTokenMatchesType() {
            assertThat(execute(LimitedGreedyWithTypedTrailingArgCmd.class, cfg -> {
            }, "broadcast hello world 5"))
                    .isSuccessful()
                    .hasArgument("message", "hello world")
                    .hasArgument("repeat", 5);
        }

        @Test
        @DisplayName("Consumes non-int token: 'hello world foo' → message='hello world foo', repeat missing → fail")
        void doesNotYieldWhenTokenDoesNotMatchType() {
            assertThat(execute(LimitedGreedyWithTypedTrailingArgCmd.class, cfg -> {
            }, "broadcast hello world foo"))
                    .hasFailed();
        }

        @Test
        @DisplayName("Limit reached then remainder goes to repeat: 'hello world foo 5' → message='hello world foo', repeat=5")
        void limitReachedThenTrailingArg() {
            assertThat(execute(LimitedGreedyWithTypedTrailingArgCmd.class, cfg -> {
            }, "broadcast hello world foo 5"))
                    .isSuccessful()
                    .hasArgument("message", "hello world foo")
                    .hasArgument("repeat", 5);
        }

        @Test
        @DisplayName("Single token then int: 'hello 5' → message='hello', repeat=5")
        void singleTokenThenInt() {
            assertThat(execute(LimitedGreedyWithTypedTrailingArgCmd.class, cfg -> {
            }, "broadcast hello 5"))
                    .isSuccessful()
                    .hasArgument("message", "hello")
                    .hasArgument("repeat", 5);
        }

        @Test
        @DisplayName("Limit reached with extra tokens: 'a b c d 5' → message='a b c', repeat=d → fail (d is not int)")
        void limitReachedExtraTokensBeforeInt() {
            assertThat(execute(LimitedGreedyWithTypedTrailingArgCmd.class, cfg -> {
            }, "broadcast a b c d 5"))
                    .hasFailed();
        }
    }
}



