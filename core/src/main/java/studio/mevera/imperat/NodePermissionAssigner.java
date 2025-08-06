package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.context.Source;

/**
 * Functional interface responsible for assigning permissions to command parameter nodes.
 * <p>
 * This interface defines how permissions loaded by a {@link PermissionLoader} should be
 * assigned to individual {@link ParameterNode} instances within a command structure.
 * The assigner is used by the command framework to automatically set appropriate
 * permissions on command parameters based on the configured permission strategy.
 * </p>
 *
 * <p>
 * Implementations of this interface determine the logic for how permissions are
 * applied to parameter nodes, allowing for flexible permission assignment strategies
 * such as hierarchical permissions, flat permission structures, or custom logic
 * based on parameter types or command context.
 * </p>
 *
 * <p>
 * The default implementation uses direct assignment via method reference to
 * {@link ParameterNode#setPermission(String)}, but custom implementations can
 * provide more sophisticated permission assignment logic.
 * </p>
 *
 * @param <S> the source type that extends {@link Source}, representing the command sender
 * @since 1.0
 * @see ParameterNode
 * @see PermissionLoader
 * @see Source
 */
public interface NodePermissionAssigner<S extends Source> {
    
    /**
     * The default delimiter used to separate permission node segments.
     * <p>
     * This constant defines the standard separator character used in hierarchical
     * permission strings (e.g., "command.subcommand.parameter").
     * </p>
     */
    String DEFAULT_DELIMITER = ".";
    
    /**
     * Gets the delimiter used to separate permission node segments.
     * <p>
     * This method returns the character or string used to separate different
     * levels in a hierarchical permission structure. The default implementation
     * returns {@link #DEFAULT_DELIMITER}.
     * </p>
     *
     * @return the permission delimiter string, never null
     * @implSpec The default implementation returns {@link #DEFAULT_DELIMITER}
     */
    default @NotNull String getPermissionDelimiter() {
        return DEFAULT_DELIMITER;
    }
    
    /**
     * Assigns the specified permission to the given parameter node.
     * <p>
     * This method is responsible for applying the permission string to the
     * parameter node using the implementation's specific assignment strategy.
     * The permission string is typically loaded by a {@link PermissionLoader}
     * and represents the required permission for accessing or executing the
     * associated command parameter.
     * </p>
     *
     * <p>
     * Implementations should handle the assignment logic appropriately, which
     * may include validation, transformation, or delegation to other components.
     * The method should be safe to call multiple times on the same node with
     * different permissions.
     * </p>
     *
     * @param node the parameter node to assign the permission to, must not be null
     * @param permission the permission string to assign, may be null to clear permissions
     * @throws NullPointerException if {@code node} is null
     * @throws IllegalArgumentException if the permission format is invalid for this assigner
     */
    void assign(@NotNull ParameterNode<S, ?> node, @Nullable String permission);
    
    /**
     * Creates a default permission assigner that directly assigns permissions to nodes.
     * <p>
     * This factory method returns a simple implementation that uses method reference
     * to {@link ParameterNode#setPermission(String)} for direct permission assignment.
     * This is the most straightforward assignment strategy and is suitable for most
     * basic use cases where no custom permission logic is required.
     * </p>
     *
     * <p>
     * The returned assigner will:
     * </p>
     * <ul>
     *   <li>Use the default permission delimiter ({@link #DEFAULT_DELIMITER})</li>
     *   <li>Directly call {@link ParameterNode#setPermission(String)} on the target node</li>
     *   <li>Pass through permission strings without modification</li>
     * </ul>
     *
     * @param <S> the source type that extends {@link Source}
     * @return a default implementation of {@code NodePermissionAssigner}, never null
     * @since 1.0
     */
    static <S extends Source> @NotNull NodePermissionAssigner<S> defaultAssigner() {
        return ParameterNode::setPermission;
    }
}
