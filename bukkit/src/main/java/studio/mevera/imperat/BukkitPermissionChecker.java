package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.permissions.PermissionChecker;

public final class BukkitPermissionChecker<S extends BukkitCommandSource> implements PermissionChecker<S> {


    /**
     * @param source     the source of the command (console or other)
     * @param permission the permission
     * @return whether this command source/sender has a specific permission
     */
    @Override
    public boolean hasPermission(
            @NotNull S source,
            @Nullable String permission
    ) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        return source.origin().hasPermission(permission);
    }

}
