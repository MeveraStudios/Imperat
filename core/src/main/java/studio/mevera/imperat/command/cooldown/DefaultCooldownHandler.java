package studio.mevera.imperat.command.cooldown;

import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.Source;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class DefaultCooldownHandler<S extends Source> implements CooldownHandler<S> {

    private final Map<String, Instant> lastTimeExecuted = new HashMap<>();
    private final CommandPathway<S> usage;

    DefaultCooldownHandler(CommandPathway<S> usage) {
        this.usage = usage;
    }


    /**
     * Sets the last time of execution to this
     * current moment using {@link System#currentTimeMillis()}
     *
     * @param source the command sender executing the {@link CommandPathway}
     */
    @Override
    public void registerExecutionMoment(S source) {
        lastTimeExecuted.put(source.name(), Instant.now());
    }

    /**
     * The required of a usage
     *
     * @return the container of usage's cooldown, the container may be empty
     */
    @Override
    public Optional<CooldownRecord> getUsageCooldown() {
        return Optional.ofNullable(usage.getCooldown());
    }

    /**
     * Unregisters the user's cached cooldown
     * when it's expired!
     *
     * @param source the command-sender
     */
    @Override
    public void removeCooldown(S source) {
        lastTimeExecuted.remove(source.name());
    }

    /**
     * Fetches the last time the command source
     * executed a specific command usage
     *
     * @param source the command sender
     * @return the last time the sender executed {@link CommandPathway}
     */
    @Override
    public Optional<Instant> getLastTimeExecuted(S source) {
        return Optional.ofNullable(lastTimeExecuted.get(source.name()));
    }
}
