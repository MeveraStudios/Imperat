package studio.mevera.imperat.tests.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import studio.mevera.imperat.context.ArgumentInput;

import java.util.List;
import java.util.stream.Stream;

/**
 * Lever 1 regression coverage: the quoted-string tokenizer
 * ({@code StringUtils#parseToQueue}) now honours backslash escapes
 * ({@code \"}, {@code \\}) inside a quoted span. Pre-fix the closing-quote
 * scan was naive and broke at the first inner {@code "}, splitting a
 * single logical token like {@code "say \"hi\" loud"} into two-or-more
 * pieces with stray backslashes embedded.
 */
@DisplayName("Quoted Tokenizer Tests")
final class QuotedTokenizerTest {

    private static List<String> tokenize(String input) {
        return List.copyOf(ArgumentInput.parse(input));
    }

    static Stream<Arguments> baselineCases() {
        return Stream.of(
                // EASY: unquoted whitespace splits as before
                Arguments.of("hello world", List.of("hello", "world")),
                // EASY: simple quoted span → one token
                Arguments.of("\"hello world\"", List.of("hello world")),
                // EASY: quoted span among other tokens
                Arguments.of("say \"hello world\" loud", List.of("say", "hello world", "loud")),
                // EASY: empty quoted span → empty string token
                Arguments.of("a \"\" b", List.of("a", "", "b")),
                // MEDIUM: leading + trailing whitespace inside quotes preserved
                Arguments.of("\"  spaces  \"", List.of("  spaces  "))
        );
    }

    @ParameterizedTest(name = "[{index}] input={0}")
    @MethodSource("baselineCases")
    @DisplayName("Baseline whitespace + quote handling preserved")
    void baselineCasesPreserved(String input, List<String> expected) {
        assertEquals(expected, tokenize(input));
    }

    // ===== HARD: escape sequences (the actual fix) =====

    @Test
    @DisplayName("Backslash-escaped inner quote is treated as literal, not as closing quote")
    void escapedInnerQuoteIsLiteral() {
        // Pre-fix: closing-quote scan stops at first `"`, splits this into
        // ["say \\\\", "hi", "\\\""] (with stray slashes).
        // Post-fix: \" is literal " inside the quoted span.
        assertEquals(
                List.of("say \"hi\" loud"),
                tokenize("\"say \\\"hi\\\" loud\"")
        );
    }

    @Test
    @DisplayName("Backslash-escaped backslash is treated as literal backslash")
    void escapedBackslashIsLiteral() {
        assertEquals(
                List.of("path\\to\\file"),
                tokenize("\"path\\\\to\\\\file\"")
        );
    }

    @Test
    @DisplayName("Mix of escaped quote + escaped backslash inside one span")
    void escapedQuoteAndBackslashMix() {
        // Input: "she said \"hi\\\""
        // Decoded: she said "hi\"
        assertEquals(
                List.of("she said \"hi\\\""),
                tokenize("\"she said \\\"hi\\\\\\\"\"")
        );
    }

    @Test
    @DisplayName("Non-quote, non-backslash escape sequences are kept verbatim (no \\n interpretation)")
    void unrecognisedEscapesKeptVerbatim() {
        // \n is NOT decoded — the tokenizer only honours \" and \\.
        // Input: "line1\nline2"  (raw 14 chars in source: " l i n e 1 \ n l i n e 2 ")
        assertEquals(
                List.of("line1\\nline2"),
                tokenize("\"line1\\nline2\"")
        );
    }

    // ===== EDGE: malformed input =====

    @Test
    @DisplayName("Unmatched opening quote falls through to literal char")
    void unmatchedOpeningQuoteFallsThrough() {
        // No closing quote → fall back to "treat as literal char", scanner
        // continues normally. Result: the opening quote stays on the token.
        assertEquals(
                List.of("\"unclosed", "next"),
                tokenize("\"unclosed next")
        );
    }

    @Test
    @DisplayName("Mid-token quote (e.g. foo\"bar baz\") is NOT treated as opener")
    void midTokenQuoteIsLiteral() {
        // The opener is only honoured when the builder is empty (no content
        // accumulated yet for the current token). This prevents identifier-like
        // inputs containing a stray quote from being split unexpectedly.
        assertEquals(
                List.of("foo\"bar", "baz\""),
                tokenize("foo\"bar baz\"")
        );
    }

    @Test
    @DisplayName("Single-quote span behaves the same way as double-quote")
    void singleQuoteSpan() {
        assertEquals(
                List.of("hello world"),
                tokenize("'hello world'")
        );
    }

    @Test
    @DisplayName("Mixed-quote span: single inside double is just a literal char")
    void singleInsideDoubleIsLiteral() {
        assertEquals(
                List.of("it's mine"),
                tokenize("\"it's mine\"")
        );
    }

    @Test
    @DisplayName("Apostrophe in unquoted token (e.g. don't) stays as one token")
    void apostropheInUnquotedTokenStaysWhole() {
        // Single quote opener with no matching close → falls through, builder
        // accumulates the quote literally.
        assertEquals(
                List.of("don't", "stop"),
                tokenize("don't stop")
        );
    }

    @Test
    @DisplayName("Multiple consecutive quoted spans separated by whitespace")
    void multipleConsecutiveQuotedSpans() {
        assertEquals(
                List.of("first one", "second one", "third"),
                tokenize("\"first one\" \"second one\" third")
        );
    }

    @Test
    @DisplayName("Adjacent quoted spans with no whitespace stay separate but concatenated literals are NOT supported")
    void adjacentQuotedSpansStaySeparate() {
        // The tokenizer treats each quoted span as a standalone token —
        // adjacent quoted spans without whitespace produce two tokens.
        // (Documented behaviour, matches Brigadier.)
        assertEquals(
                List.of("a", "b"),
                tokenize("\"a\"\"b\"")
        );
    }

    @Test
    @DisplayName("Empty input → empty token list")
    void emptyInputProducesEmptyList() {
        assertEquals(List.of(), tokenize(""));
    }

    @Test
    @DisplayName("Whitespace-only input → no tokens")
    void whitespaceOnlyInputProducesNoTokens() {
        assertEquals(List.of(), tokenize("   "));
    }

    @Test
    @DisplayName("Trailing escape at end of quoted span is preserved as backslash")
    void trailingEscapeAtEndOfSpan() {
        // "abc\" → opening quote, inner "abc\", but the \" makes the inner "
        // an escape → no actual closing quote → falls through entirely.
        // Expected: opener treated as literal, whole thing becomes one token
        // including the trailing escape.
        assertEquals(
                List.of("\"abc\\\""),
                tokenize("\"abc\\\"")
        );
    }
}
