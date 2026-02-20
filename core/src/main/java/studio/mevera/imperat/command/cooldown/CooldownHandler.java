package studio.mevera.imperat.command.cooldown;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.Source;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Cool-down checker and handler for the command usages
 * {@link CommandPathway}
 *
 * @param <S> the sender-valueType
 */
@ApiStatus.AvailableSince("1.0.0")
public interface CooldownHandler<S extends Source> {


    static <S extends Source> CooldownHandler<S> createDefault(CommandPathway<S> usage) {
        return new DefaultCooldownHandler<>(usage);
    }

    /**
     * Sets the last time of execution to this
     * current moment using {@link System#currentTimeMillis()}
     *
     * @param source the command sender executing the {@link CommandPathway}
     */
    void registerExecutionMoment(S source);

    /**
     * The required of a usage
     *
     * @return the container of usage's cooldown, the container may be empty
     */
    Optional<UsageCooldown> getUsageCooldown();

    /**
     * Checks if there's a cooldown on
     * the usage for a specific command sender
     *
     * @param source the command sender/source
     * @return whether there's a current cooldown
     * on the usage for the command sender
     */
    default boolean hasCooldown(S source) {
        UsageCooldown usageCooldown = getUsageCooldown().orElse(null);
        if (usageCooldown == null) {
            return false;
        }

        boolean result = getLastTimeExecuted(source).map((lastTime) -> {
            Duration elapsed = Duration.between(lastTime, Instant.now());
            Duration remaining = usageCooldown.toDuration().minus(elapsed);
            return !remaining.isZero() && !remaining.isNegative();
        }).orElse(false);

        if (!result) {
            removeCooldown(source);
        }
        return result;
    }

    /**
     * Unregisters the user's cached cooldown
     * when it's expired!
     *
     * @param source the command-sender
     */
    void removeCooldown(S source);

    /**
     * Fetches the last time the command source
     * executed a specific command usage
     *
     * @param source the command sender
     * @return the last time the sender executed {@link CommandPathway}
     */
    Optional<Instant> getLastTimeExecuted(S source);
}
