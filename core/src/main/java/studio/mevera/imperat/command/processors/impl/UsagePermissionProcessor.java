package studio.mevera.imperat.command.processors.impl;

import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.PermissionDeniedException;

public final class UsagePermissionProcessor<S extends Source> implements CommandPreProcessor<S> {

    UsagePermissionProcessor() {

    }
    /**
     * Processes context BEFORE the resolving operation.
     *
     * @param context the context
     * @param usage   The usage detected
     * @throws ImperatException the exception to throw if something happens
     */
    @Override
    public void process(
        Imperat<S> imperat,
        Context<S> context,
        CommandUsage<S> usage
    ) throws ImperatException {
        var source = context.source();

        if (!imperat.config().getPermissionResolver().hasUsagePermission(source, usage)) {
            throw new PermissionDeniedException();
        }

    }
}
