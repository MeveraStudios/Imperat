package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.permissions.PermissionChecker;

import java.util.regex.Pattern;

/**
 * Factory class providing common help filters based on {@link CommandPathway}.
 *
 * @author Mqzen
 */
public final class HelpFilters {

    private HelpFilters() {
    } // Prevent instantiation

    /**
     * Creates a filter based on pathway parameter count (depth).
     *
     * @param minSize minimum number of parameters (inclusive)
     * @param maxSize maximum number of parameters (inclusive)
     */
    public static <S extends CommandSource> HelpFilter<S> depth(int minSize, int maxSize) {
        return pathway -> {
            int size = pathway.size();
            return size >= minSize && size <= maxSize;
        };
    }

    /**
     * Creates a filter that checks if a source has permission for the pathway.
     *
     * @param source the source to check permissions for
     * @param checker the permission checker
     */
    public static <S extends CommandSource> HelpFilter<S> hasPermission(S source, PermissionChecker<S> checker) {
        return pathway -> checker.hasPermission(source, pathway);
    }

    /**
     * Creates a filter that checks if a source has permission for the pathway.
     *
     * @param source the source to check permissions for
     * @param context the context.
     */
    public static <S extends CommandSource> HelpFilter<S> hasPermission(S source, CommandContext<S> context) {
        return hasPermission(source, context.imperatConfig().getPermissionChecker());
    }

    /**
     * Creates a filter that checks if a source has permission for the pathway.
     *
     * @param source the source to check permissions for
     * @param help the command help.
     */
    public static <S extends CommandSource> HelpFilter<S> hasPermission(S source, CommandHelp<S> help) {
        return hasPermission(source, help.getContext());
    }

    /**
     * Creates a filter that only includes pathways with a specific permission.
     *
     * @param permission the required permission
     */
    public static <S extends CommandSource> HelpFilter<S> withPermission(String permission) {
        return pathway -> pathway.getPermissionsData().getPermissions().contains(permission);
    }

    /**
     * Creates a filter that only includes pathways without any permission requirement.
     */
    public static <S extends CommandSource> HelpFilter<S> noPermission() {
        return pathway -> pathway.getPermissionsData().isEmpty();
    }

    /**
     * Creates a filter that matches the pathway's formatted usage against a regex pattern.
     *
     * @param pattern regex pattern to match
     */
    public static <S extends CommandSource> HelpFilter<S> nameMatches(String pattern) {
        Pattern regex = Pattern.compile(pattern);
        return pathway -> regex.matcher(pathway.formatted()).matches();
    }

    /**
     * Creates a filter that matches pathways whose formatted usage contains a substring.
     *
     * @param substring the substring to search for (case-insensitive)
     */
    public static <S extends CommandSource> HelpFilter<S> nameContains(String substring) {
        String lower = substring.toLowerCase();
        return pathway -> pathway.formatted().toLowerCase().contains(lower);
    }

    /**
     * Creates a filter that only includes pathways containing at least one optional parameter.
     */
    public static <S extends CommandSource> HelpFilter<S> hasOptionalParam() {
        return pathway -> {
            for (Argument<S> arg : pathway) {
                if (arg.isOptional()) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Creates a filter that always passes (includes all pathways).
     */
    public static <S extends CommandSource> HelpFilter<S> all() {
        return pathway -> true;
    }

    /**
     * Creates a filter that never passes (excludes all pathways).
     */
    public static <S extends CommandSource> HelpFilter<S> none() {
        return pathway -> false;
    }

    /**
     * Creates a composite filter from multiple filters (AND logic).
     *
     * @param filters the filters to combine
     */
    @SafeVarargs
    public static <S extends CommandSource> HelpFilter<S> allOf(HelpFilter<S>... filters) {
        return pathway -> {
            for (HelpFilter<S> filter : filters) {
                if (!filter.filter(pathway)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Creates a composite filter from multiple filters (OR logic).
     *
     * @param filters the filters to combine
     */
    @SafeVarargs
    public static <S extends CommandSource> HelpFilter<S> anyOf(HelpFilter<S>... filters) {
        return pathway -> {
            for (HelpFilter<S> filter : filters) {
                if (filter.filter(pathway)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Creates a filter that only includes pathways with more than one parameter (i.e. not root-only).
     */
    public static <S extends CommandSource> @NotNull HelpFilter<S> childrenOnly() {
        return pathway -> pathway.size() > 1;
    }
}