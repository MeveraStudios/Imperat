package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.ParameterNode;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionChecker;

import java.util.regex.Pattern;

/**
 * Factory class providing common help filters.
 *
 * @author Mqzen
 */
public final class HelpFilters {

    private HelpFilters() {
    } // Prevent instantiation

    /**
     * Creates a filter that only includes executable nodes.
     */
    public static <S extends Source> HelpFilter<S> executable() {
        return ParameterNode::isExecutable;
    }

    /**
     * Creates a filter that only includes command nodes.
     */
    public static <S extends Source> HelpFilter<S> commands() {
        return ParameterNode::isCommand;
    }

    /**
     * Creates a filter that excludes command nodes (only arguments).
     */
    public static <S extends Source> HelpFilter<S> arguments() {
        return node -> !node.isCommand();
    }

    /**
     * Creates a filter based on node depth.
     *
     * @param minDepth minimum depth (inclusive)
     * @param maxDepth maximum depth (inclusive)
     */
    public static <S extends Source> HelpFilter<S> depth(int minDepth, int maxDepth) {
        return node -> {
            int depth = node.getDepth();
            return depth >= minDepth && depth <= maxDepth;
        };
    }

    /**
     * Creates a filter that checks if a source has permission for the node.
     *
     * @param source the source to check permissions for
     * @param checker the permission checker
     */
    public static <S extends Source> HelpFilter<S> hasPermission(S source, PermissionChecker<S> checker) {
        return node -> checker.hasPermission(source, node.getData());
    }

    /**
     * Creates a filter that checks if a source has permission for the node.
     *
     * @param source the source to check permissions for
     * @param context the context.
     */
    public static <S extends Source> HelpFilter<S> hasPermission(S source, Context<S> context) {
        return hasPermission(source, context.imperatConfig().getPermissionChecker());
    }

    /**
     * Creates a filter that checks if a source has permission for the node.
     *
     * @param source the source to check permissions for
     * @param help the command help.
     */
    public static <S extends Source> HelpFilter<S> hasPermission(S source, CommandHelp<S> help) {
        return hasPermission(source, help.getContext());
    }

    /**
     * Creates a filter that only includes nodes with a specific permission.
     *
     * @param permission the required permission
     */
    public static <S extends Source> HelpFilter<S> withPermission(String permission) {
        return node -> node.getPermissionsData().getPermissions().contains(permission);
    }

    /**
     * Creates a filter that only includes nodes without any permission requirement.
     */
    public static <S extends Source> HelpFilter<S> noPermission() {
        return node -> node.getPermissionsData().isEmpty();
    }

    /**
     * Creates a filter that matches node names against a pattern.
     *
     * @param pattern regex pattern to match
     */
    public static <S extends Source> HelpFilter<S> nameMatches(String pattern) {
        Pattern regex = Pattern.compile(pattern);
        return node -> regex.matcher(node.getData().name()).matches();
    }

    /**
     * Creates a filter that matches node names containing a substring.
     *
     * @param substring the substring to search for (case-insensitive)
     */
    public static <S extends Source> HelpFilter<S> nameContains(String substring) {
        String lower = substring.toLowerCase();
        return node -> node.getData().name().toLowerCase().contains(lower);
    }

    /**
     * Creates a filter for optional parameters only.
     */
    public static <S extends Source> HelpFilter<S> optional() {
        return ParameterNode::isOptional;
    }

    /**
     * Creates a filter for required parameters only.
     */
    public static <S extends Source> HelpFilter<S> required() {
        return ParameterNode::isRequired;
    }

    /**
     * Creates a filter that always passes (includes all nodes).
     */
    public static <S extends Source> HelpFilter<S> all() {
        return node -> true;
    }

    /**
     * Creates a filter that never passes (excludes all nodes).
     */
    public static <S extends Source> HelpFilter<S> none() {
        return node -> false;
    }

    /**
     * Creates a composite filter from multiple filters (AND logic).
     *
     * @param filters the filters to combine
     */
    @SafeVarargs
    public static <S extends Source> HelpFilter<S> allOf(HelpFilter<S>... filters) {
        return node -> {
            for (HelpFilter<S> filter : filters) {
                if (!filter.filter(node)) {
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
    public static <S extends Source> HelpFilter<S> anyOf(HelpFilter<S>... filters) {
        return node -> {
            for (HelpFilter<S> filter : filters) {
                if (filter.filter(node)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static <S extends Source> @NotNull HelpFilter<S> childrenOnly() {
        return node -> node.getParent() != null;
    }
}