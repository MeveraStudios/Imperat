package studio.mevera.imperat.resolvers;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Pair;

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

    default boolean hasPermission(@NotNull S source, @NotNull CommandParameter<S> parameter) {
        return hasPermission(source, parameter.getSinglePermission());
    }

    default Pair<String, Boolean> hasUsagePermission(S source, @Nullable CommandUsage<S> usage) {
        if (usage == null) {
            return new Pair<>(null, true);
        }

        for (var perm : usage.getPermissions()) {
            if (!hasPermission(source, perm)) {
                return new Pair<>(perm, false);
            }

        }

        return new Pair<>(null, true);
    }

}
