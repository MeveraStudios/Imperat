package studio.mevera.imperat.util;

import java.util.regex.Pattern;

public final class Patterns {

    // Allow optional `=value` suffix for the inline assignment form
    // (e.g. `--file=path.txt`, `-f=path.txt`).
    // Flag names allow alphanumerics, underscore, and internal hyphens
    // (e.g. `--no-verify`, `--save-dev`, `--all-namespaces`). The leading
    // char must be a letter/digit/underscore so the pattern doesn't eat
    // an extra hyphen from `---`.
    public final static Pattern SINGLE_FLAG = Pattern.compile("-([a-zA-Z0-9_][a-zA-Z0-9_\\-]*)(?:=(.*))?");
    public final static Pattern DOUBLE_FLAG = Pattern.compile("--([a-zA-Z0-9_][a-zA-Z0-9_\\-]*)(?:=(.*))?");

    private Patterns() {
        throw new AssertionError();
    }

    public static boolean isSingleFlag(String input) {
        return input != null && !isDoubleFlag(input) && SINGLE_FLAG.matcher(input).matches();
    }

    public static boolean isDoubleFlag(String input) {
        return input != null && DOUBLE_FLAG.matcher(input).matches();
    }

    public static boolean isInputFlag(String input) {
        return isSingleFlag(input) || isDoubleFlag(input);
    }


    public static String withoutFlagSign(String currentRaw) {
        int index = 0;
        if (isDoubleFlag(currentRaw)) {
            index = 2;
        } else if (isSingleFlag(currentRaw)) {
            index = 1;
        }
        String stripped = currentRaw.substring(index);
        int eq = stripped.indexOf('=');
        return eq >= 0 ? stripped.substring(0, eq) : stripped;
    }

    /**
     * Parses the inline-assignment value out of a flag token. Returns the
     * substring after the first {@code =}, or {@code null} when the token has
     * no inline value (the parser must consume the next stream token instead).
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "--file=path.txt"} → {@code "path.txt"}</li>
     *   <li>{@code "-f=value"} → {@code "value"}</li>
     *   <li>{@code "--verbose"} → {@code null}</li>
     *   <li>{@code "-v"} → {@code null}</li>
     * </ul>
     */
    public static String inlineFlagValue(String currentRaw) {
        if (currentRaw == null) {
            return null;
        }
        int eq = currentRaw.indexOf('=');
        if (eq < 0) {
            return null;
        }
        return currentRaw.substring(eq + 1);
    }
}
