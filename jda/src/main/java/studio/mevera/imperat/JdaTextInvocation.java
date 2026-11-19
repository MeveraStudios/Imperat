package studio.mevera.imperat;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The pure text side of a text command: finding the prefix and trigger in a message, and turning a
 * command path plus the rest of the message into Imperat input.
 */
final class JdaTextInvocation {

    private JdaTextInvocation() {
    }

    /** The trigger word (lower-cased) and everything after it. */
    record Typed(String trigger, String remainder) {
    }

    /** What to execute: the root command, its input line, and the path that runs (slash full-name style). */
    record Resolved(Command<JdaCommandSource> root, String input, String commandPath) {
    }

    /**
     * Strips a prefix (longest first) or a mention of the bot, then splits off the trigger word.
     *
     * @return the trigger and the remainder, or {@code null} when the message is not a text command
     */
    static @Nullable Typed read(String content, Collection<String> prefixes, @Nullable String botId) {
        if (content == null) return null;
        String text = content.stripLeading();
        String rest = null;
        if (botId != null) {
            for (String mention : List.of("<@" + botId + ">", "<@!" + botId + ">")) {
                if (text.startsWith(mention)) {
                    rest = text.substring(mention.length());
                    break;
                }
            }
        }
        if (rest == null) {
            List<String> ordered = new ArrayList<>(prefixes);
            ordered.removeIf(prefix -> prefix == null);
            ordered.sort(Comparator.comparingInt(String::length).reversed());
            for (String prefix : ordered) {
                if (!text.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
                String after = text.substring(prefix.length());
                // "! ban" is not a command; "@Bot ban" (above) is.
                if (!prefix.isEmpty() && !after.isEmpty() && Character.isWhitespace(after.charAt(0))) continue;
                rest = after;
                break;
            }
        }
        if (rest == null) return null;
        rest = rest.strip();
        if (rest.isEmpty()) return null;
        int split = firstWhitespace(rest);
        String trigger = split < 0 ? rest : rest.substring(0, split);
        String remainder = split < 0 ? "" : rest.substring(split).strip();
        return new Typed(trigger.toLowerCase(Locale.ROOT), remainder);
    }

    /** Splits a command path like {@code "ticket staff add"} (or slash-style {@code "a b c-d"}) into names. */
    static List<String> pathNames(String path) {
        String[] parts = path.strip().split("\\s+");
        List<String> names = new ArrayList<>();
        if (parts.length == 0 || parts[0].isEmpty()) return names;
        names.add(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            names.addAll(Arrays.asList(parts[i].split("-")));
        }
        return names;
    }

    /**
     * Builds the input for a path and what was typed after the trigger, and works out which
     * subcommand that input reaches, written as the slash command's full name would be.
     */
    static Resolved resolve(Command<JdaCommandSource> root, List<String> pathNames, String remainder) {
        List<String> words = new ArrayList<>(pathNames.subList(1, pathNames.size()));
        String input = String.join(" ", words);
        if (!remainder.isEmpty()) {
            input = input.isEmpty() ? remainder : input + " " + remainder;
            words.addAll(Arrays.asList(remainder.split("\\s+")));
        }

        List<String> reached = new ArrayList<>();
        reached.add(root.getName());
        Command<JdaCommandSource> current = root;
        for (String word : words) {
            Command<JdaCommandSource> next = null;
            for (Command<JdaCommandSource> sub : current.getSubCommands()) {
                if (sub.hasName(word)) {
                    next = sub;
                    break;
                }
            }
            if (next == null) break;
            reached.add(next.getName());
            current = next;
        }
        return new Resolved(root, input, slashStyle(reached));
    }

    private static String slashStyle(List<String> names) {
        if (names.size() <= 3) return String.join(" ", names).toLowerCase(Locale.ROOT);
        return (names.get(0) + " " + names.get(1) + " " + String.join("-", names.subList(2, names.size())))
                       .toLowerCase(Locale.ROOT);
    }

    private static int firstWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) return i;
        }
        return -1;
    }
}
