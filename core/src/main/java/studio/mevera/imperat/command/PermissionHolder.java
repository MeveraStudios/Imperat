package studio.mevera.imperat.command;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an entity that can hold a permission.
 * This interface allows getting and setting a permission string.
 */
public interface PermissionHolder {

    /**
     * Retrieves the permission associated with this holder.
     *
     * @return the permission string, or {@code null} if no permission is set.
     */
    @Nullable
    String permission();

    /**
     * Sets the permission for this holder.
     *
     * @param permission the permission string to set, can be {@code null}.
     */
    void permission(String permission);

}
