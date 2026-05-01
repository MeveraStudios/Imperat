package studio.mevera.imperat.providers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BungeeCommandSource;
import studio.mevera.imperat.permissions.PermissionChecker;

public class BungeePermissionChecker<S extends BungeeCommandSource> implements PermissionChecker<S> {

    @Override
    public boolean hasPermission(
            @NotNull S source,
            @Nullable String permission
    ) {
        if (source.isConsole() || permission == null) {
            return true;
        }

        return source.origin().hasPermission(permission);
    }
}
