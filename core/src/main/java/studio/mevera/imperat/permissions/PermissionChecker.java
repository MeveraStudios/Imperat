package studio.mevera.imperat.permissions;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Source;

/**
 * Represents a functional way of checking for the permissions
 * of the command source/sender.
 */
@ApiStatus.AvailableSince("1.0.0")
public interface PermissionChecker<S extends Source> {

    /**
     * @param source     the source of the command (console or other)
     * @param permission the permission
     * @return whether this command source/sender has a specific permission
     */
    boolean hasPermission(@NotNull S source, @Nullable String permission);

    default boolean hasPermission(@NotNull S source, @NotNull PermissionHolder holder) {

        PermissionsData data = holder.getPermissionsData();
        CommandPermissionCondition permissionCondition = data.getCondition();

        return permissionCondition.has(source, this);
    }

}
