package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.base.InstanceFactory;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.permissions.PermissionChecker;

public final class RuntimeConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    RuntimeConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public RuntimeConfig<S> eventBus(EventBus bus) {
        config.setEventBus(bus);
        return this;
    }

    public RuntimeConfig<S> permissionChecker(PermissionChecker<S> permissionChecker) {
        config.setPermissionResolver(permissionChecker);
        return this;
    }

    public RuntimeConfig<S> instanceFactory(InstanceFactory<S> instanceFactory) {
        config.setInstanceFactory(instanceFactory);
        return this;
    }

    public RuntimeConfig<S> throwablePrinter(@NotNull ThrowablePrinter printer) {
        config.setThrowablePrinter(printer);
        return this;
    }

    public RuntimeConfig<S> coroutineScope(@NotNull Object scope) {
        config.setCoroutineScope(scope);
        return this;
    }
}
