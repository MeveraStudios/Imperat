package studio.mevera.imperat.type;

/** Reads a Discord ID from typed input: a raw ID or a mention ({@code <@id>}, {@code <@!id>}, {@code <@&id>}, {@code <#id>}). */
final class Snowflakes {

    private Snowflakes() {
    }

    /** @return the ID, or {@code 0} when the input is not an ID or a mention */
    static long parse(String input) {
        String text = input.strip();
        if (text.startsWith("<") && text.endsWith(">")) {
            text = text.substring(1, text.length() - 1);
            if (text.startsWith("@&")) text = text.substring(2);
            else if (text.startsWith("@!")) text = text.substring(2);
            else if (text.startsWith("@") || text.startsWith("#")) text = text.substring(1);
            else return 0;
        }
        if (!text.matches("\\d{17,20}")) return 0;
        try {
            return Long.parseUnsignedLong(text);
        } catch (NumberFormatException tooLarge) {
            return 0;
        }
    }
}
