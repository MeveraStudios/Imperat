package studio.mevera.imperat.tests.parameters;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {


    /**
     * Converts a duration string into milliseconds. The supported units are years, months, days, hours, and seconds.
     * Returns -1 for permanent durations.
     *
     * @param durationString The duration string to be converted.
     * @return The duration in milliseconds.
     */
    public static long convertDurationToMs(final String durationString) {
        if (durationString.toLowerCase().startsWith("perm")) {
            return -1;
        }

        long milliseconds = 0;
        Pattern pattern = Pattern.compile("(\\d+)\\s*(\\w+)");
        Matcher matcher = pattern.matcher(durationString);

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            switch (unit) {
                case "y":
                case "year":
                case "years":
                    milliseconds += (long) (365.25 * value * TimeUnit.DAYS.toMillis(1));
                    break;
                case "m":
                case "mo":
                case "month":
                case "months":
                    milliseconds += (long) (30.4375 * value * TimeUnit.DAYS.toMillis(1));
                    break;
                case "d":
                case "day":
                case "days":
                    milliseconds += TimeUnit.DAYS.toMillis(value);
                    break;
                case "h":
                case "hour":
                case "hours":
                    milliseconds += TimeUnit.HOURS.toMillis(value);
                    break;
                case "s":
                case "second":
                case "seconds":
                    milliseconds += TimeUnit.SECONDS.toMillis(value);
                    break;
                default:
                    return 0;
            }
        }
        return milliseconds;
    }


    /**
     * Converts a time duration in milliseconds into a human-readable format, including years, months, days, hours,
     * minutes, and seconds.
     *
     * @param timeLeftMs The time duration in milliseconds.
     * @param big        True for a detailed format, false for a shorter format.
     * @return The human-readable time format.
     */
    public static String calculateTimeLeft(long timeLeftMs, boolean big) {
        if (timeLeftMs == -1) {
            return "Permanent";
        }

        if (timeLeftMs < 1000) { // Less than a second
            return "Now";
        }

        long years = timeLeftMs / (long) (365.25 * TimeUnit.DAYS.toMillis(1));
        timeLeftMs %= (long) (365.25 * TimeUnit.DAYS.toMillis(1));

        long months = timeLeftMs / (long) (30.4375 * TimeUnit.DAYS.toMillis(1));
        timeLeftMs %= (long) (30.4375 * TimeUnit.DAYS.toMillis(1));

        long days = timeLeftMs / TimeUnit.DAYS.toMillis(1);
        timeLeftMs %= TimeUnit.DAYS.toMillis(1);

        long hours = timeLeftMs / TimeUnit.HOURS.toMillis(1);
        timeLeftMs %= TimeUnit.HOURS.toMillis(1);

        long minutes = timeLeftMs / TimeUnit.MINUTES.toMillis(1);
        timeLeftMs %= TimeUnit.MINUTES.toMillis(1);

        long seconds = timeLeftMs / TimeUnit.SECONDS.toMillis(1);

        StringBuilder result = new StringBuilder();

        if (years > 0) {
            result.append(years).append(big ? " year" : "y");
            if (big && years > 1) {
                result.append("s");
            }
        }

        if (months > 0) {
            if (!result.isEmpty()) {
                result.append(big ? ", " : " ");
            }
            result.append(months).append(big ? " month" : "mo");
            if (big && months > 1) {
                result.append("s");
            }
        }

        if (days > 0) {
            if (!result.isEmpty()) {
                result.append(big ? ", " : " ");
            }
            result.append(days).append(big ? " day" : "d");
            if (big && days > 1) {
                result.append("s");
            }
        }

        if (hours > 0) {
            if (!result.isEmpty()) {
                result.append(big ? ", " : " ");
            }
            result.append(hours).append(big ? " hour" : "h");
            if (big && hours > 1) {
                result.append("s");
            }
        }

        if (minutes > 0) {
            if (!result.isEmpty()) {
                result.append(big ? ", " : " ");
            }
            result.append(minutes).append(big ? " minute" : "m");
            if (big && minutes > 1) {
                result.append("s");
            }
        }

        if (seconds > 0) {
            if (!result.isEmpty()) {
                result.append(big ? ", " : " ");
            }
            result.append(seconds).append(big ? " second" : "s");
            if (big && seconds > 1) {
                result.append("s");
            }
        }
        return result.toString();
    }
}