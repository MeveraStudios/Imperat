package studio.mevera.imperat.tests.parameters;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaDurationParser {

    // Regex pattern to match number followed by time unit (d/D, h/H, m/M, s/S)
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([dhmsHDMS])");

    /**
     * Parses a duration string and returns a CustomDuration object.
     * Supported formats:
     * - "24h" or "24H" for 24 hours
     * - "1m" or "1M" for 1 minute
     * - "230d" or "230D" for 230 days
     * - "27d15h10m30m11s99s" for complex durations (case insensitive, accumulates duplicate units)
     *
     * @param durationStr the duration string to parse
     * @return CustomDuration object representing the parsed duration
     * @throws IllegalArgumentException if the string format is invalid
     */
    public static Duration parseDuration(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            throw new IllegalArgumentException("CustomDuration string cannot be null or empty");
        }
        
        if(durationStr.startsWith("perm")) {
            return Duration.ZERO;
        }

        // Remove whitespace (keep original case for unit matching)
        String normalized = durationStr.trim();

        // Accumulators for each time unit
        long totalDays = 0;
        long totalHours = 0;
        long totalMinutes = 0;
        long totalSeconds = 0;

        Matcher matcher = DURATION_PATTERN.matcher(normalized);
        boolean foundMatch = false;

        // Find all matches and accumulate values
        while (matcher.find()) {
            foundMatch = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit.toLowerCase()) {
                case "d":
                    totalDays += value;
                    break;
                case "h":
                    totalHours += value;
                    break;
                case "m":
                    totalMinutes += value;
                    break;
                case "s":
                    totalSeconds += value;
                    break;
            }
        }

        if (!foundMatch) {
            throw new IllegalArgumentException("Invalid customDuration format: " + durationStr);
        }

        // Validate that the entire string was consumed (no invalid characters)
        String withoutMatches = matcher.replaceAll("");
        if (!withoutMatches.isEmpty()) {
            throw new IllegalArgumentException("Invalid characters in customDuration string: " + durationStr);
        }

        // Build CustomDuration object by adding all components
        Duration customDuration = Duration.ZERO;
        customDuration = customDuration.plusDays(totalDays);
        customDuration = customDuration.plusHours(totalHours);
        customDuration = customDuration.plusMinutes(totalMinutes);
        customDuration = customDuration.plusSeconds(totalSeconds);

        return customDuration;
    }

    /**
     * Formats a CustomDuration object into a human-readable string.
     * Examples:
     * - 24 hours -> "24 hours"
     * - 1 hour -> "1 hour" (singular)
     * - 1 day, 15 minutes, 10 seconds -> "1 day, 15 minutes, and 10 seconds"
     * - 0 seconds -> "0 seconds"
     *
     * @param duration the CustomDuration object to format
     * @return formatted human-readable string
     */
    public static String formatDuration(java.time.Duration duration) {
        if (duration == null) {
            return "0 seconds";
        }

        // Extract components
        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        // Handle zero duration
        if (totalSeconds == 0) {
            return "0 seconds";
        }

        // Build components list
        java.util.List<String> components = new java.util.ArrayList<>();

        if (days > 0) {
            components.add(days == 1 ? "1 day" : days + " days");
        }
        if (hours > 0) {
            components.add(hours == 1 ? "1 hour" : hours + " hours");
        }
        if (minutes > 0) {
            components.add(minutes == 1 ? "1 minute" : minutes + " minutes");
        }
        if (seconds > 0) {
            components.add(seconds == 1 ? "1 second" : seconds + " seconds");
        }

        // Format the result with proper grammar
        if (components.size() == 1) {
            return components.get(0);
        } else if (components.size() == 2) {
            return components.get(0) + " and " + components.get(1);
        } else {
            // Join all but last with commas, then add "and" before the last
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < components.size() - 1; i++) {
                if (i > 0) result.append(", ");
                result.append(components.get(i));
            }
            result.append(", and ").append(components.get(components.size() - 1));
            return result.toString();
        }
    }

}