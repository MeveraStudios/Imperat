package studio.mevera.imperat.exception;

import studio.mevera.imperat.context.Context;

import java.time.Duration;
import java.time.Instant;

public final class CooldownException extends ImperatException {

    private final Duration cooldownDuration, remainingDuration;
    private final Instant lastTimeExecuted;

    public CooldownException(final Duration cooldownDuration, Instant lastTimeExecuted, Context<?> ctx) {
        super(ctx);
        this.cooldownDuration = cooldownDuration;
        this.lastTimeExecuted = lastTimeExecuted;

        Duration elapsed = Duration.between(lastTimeExecuted, Instant.now());
        var remaining = cooldownDuration.minus(elapsed);
        this.remainingDuration = remaining.isNegative()
                ? Duration.ZERO
                : remaining;
    }


    public Duration getCooldownDuration() {
        return cooldownDuration;
    }

    public Instant getLastTimeExecuted() {
        return lastTimeExecuted;
    }

    public Duration getRemainingDuration() {
        return remainingDuration;
    }

}
