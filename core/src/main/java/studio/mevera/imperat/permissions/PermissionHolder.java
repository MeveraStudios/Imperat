package studio.mevera.imperat.permissions;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an entity that can hold a permission.
 * This interface allows getting and setting a permission string.
 */
public interface PermissionHolder {

    @NotNull PermissionsData getPermissionsData();

    void setPermissionData(@NotNull PermissionsData permission);
}
