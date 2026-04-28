package studio.mevera.imperat.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.ArgumentInput;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    public final static char DOUBLE_QUOTE = '"', SINGLE_QUOTE = '\'';

    /**
     * Pattern to extract snowflake IDs. Useful for JDA
     */
    public static final Pattern SNOWFLAKE = Pattern.compile("<(@!|@|@&|#)(?<snowflake>\\d{18})>");

    /**
     * General utilities for string operations
     */
    private StringUtils() {
    }

    public static @Nullable String getSnowflake(String mention) {
        Matcher matcher = SNOWFLAKE.matcher(mention);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    public static @NotNull String stripNamespace(@NotNull String command) {
        int colon = command.indexOf(':');
        if (colon == -1) {
            return command;
        }
        return command.substring(colon + 1);
    }

    public static String normalizedParameterFormatting(String parameterContent, boolean optional) {
        String prefix, suffix;
        if (optional) {
            prefix = "[";
            suffix = "]";
        } else {
            prefix = "<";
            suffix = ">";
        }
        return prefix + parameterContent + suffix;
    }

    public static ArgumentInput parseToQueue(String argumentsInOneLine, boolean autoCompletion, boolean extraSpace) {
        // "add "
        if (argumentsInOneLine.isEmpty()) {
            return !autoCompletion ? ArgumentInput.of(argumentsInOneLine) : ArgumentInput.parse(" ");
        }

        ArgumentInput toCollect = ArgumentInput.of(argumentsInOneLine);
        char[] chars = argumentsInOneLine.toCharArray();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (isQuoteChar(c) && i != chars.length - 1 && builder.isEmpty()) {
                // Quoted span: scan ahead for the matching end quote, honouring
                // backslash escapes (\" → literal ", \\ → literal \). Only
                // treats this as a quote opener when the builder is empty —
                // mid-token quotes (e.g. foo"bar") are kept as literal chars
                // so identifier-like inputs are not split unexpectedly.
                int endQuoteIndex = findClosingQuote(chars, i + 1, c);

                if (endQuoteIndex != -1) {
                    int j = i + 1;
                    while (j < endQuoteIndex) {
                        char ch = chars[j];
                        if (ch == '\\' && j + 1 < endQuoteIndex) {
                            char next = chars[j + 1];
                            if (next == c || next == '\\') {
                                builder.append(next);
                                j += 2;
                                continue;
                            }
                        }
                        builder.append(ch);
                        j++;
                    }
                    i = endQuoteIndex;
                    toCollect.add(builder.toString());
                    builder = new StringBuilder();
                    continue;
                }
                // If no matching end quote, fall through to treat as regular character
            }

            if (Character.isWhitespace(c)) {

                if (!builder.isEmpty()) {
                    toCollect.add(builder.toString());
                    if (autoCompletion && i == chars.length - 1) {
                        toCollect.add(String.valueOf(c));
                    }
                    builder = new StringBuilder();
                }

                continue;
            }

            builder.append(c);
        }

        // Don't forget to add any remaining content
        if (!builder.isEmpty()) {
            toCollect.add(builder.toString());
        }

        if (autoCompletion && extraSpace) {
            toCollect.add(" ");
        }

        return toCollect;
    }

    /**
     * Returns the index of the matching closing quote for the opener at
     * {@code searchFrom - 1}, or {@code -1} if none exists. Treats
     * {@code \"} and {@code \\} as escaped literals so the closing scan
     * does not stop on an escaped quote.
     */
    private static int findClosingQuote(char[] chars, int searchFrom, char opener) {
        for (int j = searchFrom; j < chars.length; j++) {
            char ch = chars[j];
            if (ch == '\\' && j + 1 < chars.length) {
                char next = chars[j + 1];
                if (next == opener || next == '\\') {
                    j++; // skip escaped char
                    continue;
                }
            }
            if (isEndOfQuote(opener, ch)) {
                return j;
            }
        }
        return -1;
    }

    public static ArgumentInput parseToQueue(String argumentsInOneLine, boolean autoCompletion) {
        return parseToQueue(argumentsInOneLine, autoCompletion, false);
    }


    public static boolean isQuoteChar(char ch) {
        return ch == DOUBLE_QUOTE || ch == SINGLE_QUOTE;
    }

    public static boolean isEndOfQuote(char quoteStarter, char value) {
        return isQuoteChar(value) && isQuoteChar(quoteStarter) && quoteStarter == value;
    }
}

