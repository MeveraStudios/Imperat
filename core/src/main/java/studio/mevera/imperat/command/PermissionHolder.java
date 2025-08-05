package studio.mevera.imperat.command;

import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

/**
 * Represents an entity that can hold a permission.
 * This interface allows getting and setting a permission string.
 */
public interface PermissionHolder {

    /**
     * Retrieves the permission associated with this holder.
     *
     * @return A set of permission strings, if no permissions are set, it will return an empty set.
     */
    @Unmodifiable Set<String> getPermissions();

    /**
     * Adds a permission for this holder.
     *
     * @param permission the permission string to add, can be {@code null}.
     */
    void addPermission(String permission);
    
    default boolean hasPermission(String permission) {
        return getPermissions().contains(permission);
    }
}
