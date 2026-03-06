package studio.mevera.imperat.command.processors.impl;

import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;

import java.time.Duration;
import java.time.Instant;

public final class CooldownProcessor<S extends Source> implements CommandPostProcessor<S> {

    CooldownProcessor() {

    }

    /**
     * Processes context BEFORE the resolving operation.
     *
     * @param context the context
     * @throws CommandException the exception to throw if something happens
     */
    @Override
    public void process(
            ExecutionContext<S> context
    ) throws CommandException {
        var source = context.source();
        var pathway = context.getDetectedPathway();
        var handler = pathway.getCooldownHandler();
        var cooldown = pathway.getCooldown();

        if (handler.hasCooldown(source)) {
            assert cooldown != null;
            if (cooldown.permission() == null
                        || cooldown.permission().isEmpty()
                        || !context.imperatConfig().getPermissionChecker().hasPermission(source, cooldown.permission())) {

                var cooldownDuration = cooldown.toDuration();
                var lastTimeExecuted = handler.getLastTimeExecuted(source).orElseThrow();
                var elapsed = Duration.between(lastTimeExecuted, Instant.now());
                var remaining = cooldownDuration.minus(elapsed);
                var remainingDuration = remaining.isNegative() ? Duration.ZERO : remaining;

                throw ResponseException.of(ResponseKey.COOLDOWN)
                              .withPlaceholder("seconds", String.valueOf(remainingDuration.toSeconds()))
                              .withPlaceholder("remaining_duration", remainingDuration.toString())
                              .withPlaceholder("cooldown_duration", cooldownDuration.toString())
                              .withPlaceholder("last_executed", lastTimeExecuted.toString());
            }
        }
        handler.registerExecutionMoment(source);
    }

}
