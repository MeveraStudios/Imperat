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
        if (matcher.find())
            return matcher.group(2);
        return null;
    }

    public static @NotNull String stripNamespace(@NotNull String command) {
        int colon = command.indexOf(':');
        if (colon == -1)
            return command;
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
        if (argumentsInOneLine.isEmpty())
            return !autoCompletion ? ArgumentInput.of(argumentsInOneLine) : ArgumentInput.parse(" ");
        
        ArgumentInput toCollect = ArgumentInput.of(argumentsInOneLine);
        char[] chars = argumentsInOneLine.toCharArray();
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            
            if (isQuoteChar(c) && i != chars.length - 1) {
                // Check if there's a matching end quote
                int endQuoteIndex = -1;
                for (int j = i + 1; j < chars.length; j++) {
                    if (isEndOfQuote(c, chars[j])) {
                        endQuoteIndex = j;
                        break;
                    }
                }
                
                // Only treat as quoted section if matching end quote exists
                if (endQuoteIndex != -1) {
                    // Add any content in builder before starting quoted section
                    if (!builder.isEmpty()) {
                        toCollect.add(builder.toString());
                        builder = new StringBuilder();
                    }
                    
                    // Collect quoted content
                    int start = i + 1;
                    while (start < endQuoteIndex) {
                        builder.append(chars[start]);
                        start++;
                    }
                    i = endQuoteIndex; // Skip past the end quote
                    
                    toCollect.add(builder.toString());
                    builder = new StringBuilder();
                    continue;
                }
                // If no matching end quote, fall through to treat as regular character
            }
            
            if (Character.isWhitespace(c)) {
                
                if (!builder.isEmpty()) {
                    toCollect.add(builder.toString());
                    if(autoCompletion && i == chars.length-1) {
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
        
        if(autoCompletion && extraSpace) {
            toCollect.add(" ");
        }
        
        return toCollect;
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

