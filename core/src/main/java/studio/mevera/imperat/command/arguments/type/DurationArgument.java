package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses an input token into a {@link Duration}. Accepts both ISO-8601
 * (e.g. {@code PT1H30M}, {@code P2D}) and relaxed shorthand
 * (e.g. {@code 1h30m}, {@code 2d}, {@code 27d15h10m30s}).
 *
 * <p>Relaxed-form units are case-insensitive: {@code d} (days), {@code h}
 * (hours), {@code m} (minutes), {@code s} (seconds). Multiple components
 * accumulate ({@code 1h30m} = 90 minutes). Whitespace inside the token is
 * tolerated.</p>
 */
public final class DurationArgument<S extends CommandSource> extends SimpleArgumentType<S, Duration> {

    private static final Pattern RELAXED_PATTERN = Pattern.compile("(\\d+)([dhmsDHMS])");

    @Override
    public Duration parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            throw new CommandException("Duration cannot be empty");
        }

        // Try ISO-8601 first; many users will type "PT5M" verbatim.
        try {
            return Duration.parse(trimmed);
        } catch (Exception ignored) {
            // fall through to relaxed parser
        }

        Matcher matcher = RELAXED_PATTERN.matcher(trimmed.replace(" ", ""));
        long days = 0, hours = 0, minutes = 0, seconds = 0;
        boolean matched = false;
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                throw new CommandException("Invalid duration format: '%s'", input);
            }
            matched = true;
            long value = Long.parseLong(matcher.group(1));
            switch (matcher.group(2).toLowerCase()) {
                case "d" -> days += value;
                case "h" -> hours += value;
                case "m" -> minutes += value;
                case "s" -> seconds += value;
            }
            lastEnd = matcher.end();
        }

        if (!matched || lastEnd != trimmed.replace(" ", "").length()) {
            throw new CommandException("Invalid duration format: '%s'", input);
        }

        return Duration.ZERO
                       .plusDays(days)
                       .plusHours(hours)
                       .plusMinutes(minutes)
                       .plusSeconds(seconds);
    }
}
