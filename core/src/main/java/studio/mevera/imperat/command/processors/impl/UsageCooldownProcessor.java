package studio.mevera.imperat.command.processors.impl;

import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.CooldownException;

public final class UsageCooldownProcessor<S extends Source> implements CommandPreProcessor<S> {

    UsageCooldownProcessor() {

    }
    /**
     * Processes context BEFORE the resolving operation.
     *
     * @param imperat the api
     * @param context the context
     * @param usage   The usage detected
     * @throws CommandException the exception to throw if something happens
     */
    @Override
    public void process(
        Imperat<S> imperat,
        Context<S> context,
        CommandUsage<S> usage
    ) throws CommandException {
        var source = context.source();
        var handler = usage.getCooldownHandler();
        var cooldown = usage.getCooldown();

        if (handler.hasCooldown(source)) {
            assert cooldown != null;
            if(cooldown.permission() == null
                    || cooldown.permission().isEmpty()
                    || !imperat.config().getPermissionChecker().hasPermission(source, cooldown.permission())) {


                throw new CooldownException(
                        cooldown.toDuration(),
                        handler.getLastTimeExecuted(source).orElseThrow()
                );
            }
        }
        handler.registerExecutionMoment(source);
    }

}
