package studio.mevera.imperat.command.processors.impl;

import studio.mevera.imperat.context.Source;

public final class DefaultProcessors {

    public static <S extends Source> UsageCooldownProcessor<S> preUsageCooldown() {
        return new UsageCooldownProcessor<>();
    }

    public static <S extends Source> UsagePermissionProcessor<S> preUsagePermission() {
        return new UsagePermissionProcessor<>();
    }
}
