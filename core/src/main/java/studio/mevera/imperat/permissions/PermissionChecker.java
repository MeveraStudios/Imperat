package studio.mevera.imperat.permissions;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.Pair;

/**
 * Represents a functional way of checking for the permissions
 * of the command source/sender.
 */
@ApiStatus.AvailableSince("1.0.0")
public interface PermissionChecker<S extends CommandSource> {

    /**
     * @param source     the source of the command (console or other)
     * @param permission the permission
     * @return whether this command source/sender has a specific permission
     */
    boolean hasPermission(@NotNull S source, @Nullable String permission);

    default boolean hasPermission(@NotNull S source, @NotNull PermissionHolder holder) {
        return checkPermission(source, holder).right();
    }

    default Pair<PermissionHolder, Boolean> checkPermission(@NotNull S source, @NotNull PermissionHolder holder) {
        PermissionsData data = holder.getPermissionsData();
        CommandPermissionCondition permissionCondition = data.getCondition();
        Pair<String, Boolean> result = permissionCondition.check(source, this);

        return new Pair<>(result.right() ? null : holder, result.right());
    }

}
