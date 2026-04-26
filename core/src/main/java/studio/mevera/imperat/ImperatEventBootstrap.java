package studio.mevera.imperat;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.events.ExecutionStrategy;
import studio.mevera.imperat.events.types.CommandPostProcessEvent;
import studio.mevera.imperat.events.types.CommandPreProcessEvent;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.util.priority.Priority;

import java.time.Duration;
import java.time.Instant;

final class ImperatEventBootstrap<S extends CommandSource> {

    private final Imperat<S> imperat;
    private final ImperatConfig<S> config;

    ImperatEventBootstrap(Imperat<S> imperat, ImperatConfig<S> config) {
        this.imperat = imperat;
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    void registerDefaultListeners() {
        imperat.listen(CommandPreProcessEvent.class, (event) -> {
            Command<S> command = event.getCommand();
            CommandContext<S> context = event.getContext();
            try {
                command.preProcess(context);
            } catch (CommandException e) {
                event.setCancelled(true);
                throw new RuntimeException(e);
            }
        }, Priority.NORMAL, ExecutionStrategy.SYNC);

        imperat.listen(CommandPostProcessEvent.class, (event) -> {
            Command<S> command = event.getCommand();
            ExecutionContext<S> context = event.getContext();
            try {
                command.postProcess(context);
            } catch (CommandException e) {
                throw new RuntimeException(e);
            }
        }, Priority.NORMAL, ExecutionStrategy.SYNC);

        imperat.listen(CommandPostProcessEvent.class, (event) -> {
            ExecutionContext<S> context = event.getContext();
            S source = context.source();
            var pathway = context.getDetectedPathway();
            var handler = pathway.getCooldownHandler();
            var cooldown = pathway.getCooldown();

            if (handler.hasCooldown(source)) {
                assert cooldown != null;
                if (cooldown.permission() == null
                            || cooldown.permission().isEmpty()
                            || !config.getPermissionChecker().hasPermission(source, cooldown.permission())) {

                    var cooldownDuration = cooldown.toDuration();
                    Instant lastTimeExecuted = (Instant) handler.getLastTimeExecuted(source).orElseThrow();
                    var elapsed = Duration.between(lastTimeExecuted, Instant.now());
                    var remaining = cooldownDuration.minus(elapsed);
                    var remainingDuration = remaining.isNegative() ? Duration.ZERO : remaining;

                    event.setCancelled(true);
                    throw ResponseException.of(ResponseKey.COOLDOWN)
                                  .withPlaceholder("seconds", String.valueOf(remainingDuration.toSeconds()))
                                  .withPlaceholder("remaining_duration", remainingDuration.toString())
                                  .withPlaceholder("cooldown_duration", cooldownDuration.toString())
                                  .withPlaceholder("last_executed", lastTimeExecuted.toString());
                }
            }
            handler.registerExecutionMoment(source);
        }, Priority.NORMAL, ExecutionStrategy.SYNC);
    }
}
