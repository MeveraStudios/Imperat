package studio.mevera.imperat.permissions;

import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPermissionCondition {

    private final static String AND_SYMBOL = "&";
    private final static String OR_SYMBOL = "|";
    private final static String NOT_SYMBOL = "!";
    private final List<CommandPermissionCondition> children = new ArrayList<>();
    private final String permission;  // leaf node
    private final Operator operator;
    // --- Constructors ---
    private CommandPermissionCondition() {  // empty constructor
        this.permission = null;
        this.operator = null;
    }

    private CommandPermissionCondition(String permission) { // leaf node
        this.permission = permission.trim();
        this.operator = null;
    }

    private CommandPermissionCondition(Operator operator, CommandPermissionCondition... conditions) { // composite
        this.permission = null;
        this.operator = operator;
        this.children.addAll(Arrays.asList(conditions));
        if (operator == Operator.NOT && children.size() != 1) {
            throw new IllegalArgumentException("NOT operator must have exactly one child");
        }
    }

    public static CommandPermissionCondition has(String perm) {
        return new CommandPermissionCondition(perm);
    }

    // --- Static helpers: all / allOr for strings ---
    public static CommandPermissionCondition all(List<String> perms) {
        if (perms.isEmpty()) {
            throw new IllegalArgumentException("Permission list cannot be empty");
        }
        CommandPermissionCondition result = new CommandPermissionCondition(perms.get(0));
        for (int i = 1; i < perms.size(); i++) {
            result = result.and(perms.get(i));
        }
        return result;
    }

    public static CommandPermissionCondition all(String... perms) {
        return all(Arrays.asList(perms));
    }

    public static CommandPermissionCondition allOr(List<String> perms) {
        if (perms.isEmpty()) {
            throw new IllegalArgumentException("Permission list cannot be empty");
        }
        CommandPermissionCondition result = new CommandPermissionCondition(perms.get(0));
        for (int i = 1; i < perms.size(); i++) {
            result = result.or(perms.get(i));
        }
        return result;
    }

    public static CommandPermissionCondition allOr(String... perms) {
        return allOr(Arrays.asList(perms));
    }

    // --- Static helpers: all / allOr for conditions ---
    public static CommandPermissionCondition all(CommandPermissionCondition... conditions) {
        if (conditions.length == 0) {
            throw new IllegalArgumentException("Condition list cannot be empty");
        }
        CommandPermissionCondition result = conditions[0];
        for (int i = 1; i < conditions.length; i++) {
            result = new CommandPermissionCondition(Operator.AND, result, conditions[i]);
        }
        return result;
    }

    public static CommandPermissionCondition allOr(CommandPermissionCondition... conditions) {
        if (conditions.length == 0) {
            throw new IllegalArgumentException("Condition list cannot be empty");
        }
        CommandPermissionCondition result = conditions[0];
        for (int i = 1; i < conditions.length; i++) {
            result = new CommandPermissionCondition(Operator.OR, result, conditions[i]);
        }
        return result;
    }

    public static CommandPermissionCondition empty() {
        return new CommandPermissionCondition() {
            @Override
            public <S extends Source> boolean has(S source, PermissionChecker<S> checker) {
                return true;
            }

            @Override
            public List<String> collectPermissionsUsedOnConditions() {
                return Collections.emptyList();
            }

            @Override
            public String toString() {
                return "TRUE";
            }
        };
    }

    // --- Text parser for AND, OR, NOT ---
    public static CommandPermissionCondition fromText(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            return empty();
        }

        text = stripOuterParens(text);

        // OR (lowest precedence)
        List<String> orParts = splitOutsideParens(text, OR_SYMBOL.charAt(0));
        if (orParts.size() > 1) {
            CommandPermissionCondition result = fromText(orParts.get(0));
            for (int i = 1; i < orParts.size(); i++) {
                result = new CommandPermissionCondition(Operator.OR, result, fromText(orParts.get(i)));
            }
            return result;
        }

        // AND
        List<String> andParts = splitOutsideParens(text, AND_SYMBOL.charAt(0));
        if (andParts.size() > 1) {
            CommandPermissionCondition result = fromText(andParts.get(0));
            for (int i = 1; i < andParts.size(); i++) {
                result = new CommandPermissionCondition(Operator.AND, result, fromText(andParts.get(i)));
            }
            return result;
        }

        // NOT (highest precedence)
        if (text.startsWith(NOT_SYMBOL)) {
            String remainder = text.substring(NOT_SYMBOL.length()).trim();
            if (remainder.isEmpty()) {
                throw new IllegalArgumentException("NOT operator must have a target");
            }
            return new CommandPermissionCondition(Operator.NOT, fromText(remainder));
        }

        // Leaf
        return new CommandPermissionCondition(text);
    }

    private static List<String> splitOutsideParens(String text, char symbol) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int lastSplit = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == symbol && depth == 0) {
                result.add(text.substring(lastSplit, i).trim());
                lastSplit = i + 1;
            }
        }

        result.add(text.substring(lastSplit).trim());
        return result;
    }

    private static String stripOuterParens(String text) {
        while (text.length() >= 2 && text.charAt(0) == '(' && text.charAt(text.length() - 1) == ')') {
            int depth = 0;
            boolean wraps = true;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                if (depth == 0 && i < text.length() - 1) {
                    wraps = false;
                    break;
                }
            }
            if (!wraps) {
                break;
            }
            text = text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    // --- Fluent API (String versions) ---
    public CommandPermissionCondition and(String... perms) {
        return new CommandPermissionCondition(Operator.AND, this, all(perms));
    }

    public CommandPermissionCondition or(String... perms) {
        return new CommandPermissionCondition(Operator.OR, this, allOr(perms));
    }

    // --- Fluent API (Condition versions) ---
    public CommandPermissionCondition and(CommandPermissionCondition... conditions) {
        return new CommandPermissionCondition(Operator.AND, this, all(conditions));
    }

    public CommandPermissionCondition or(CommandPermissionCondition... conditions) {
        return new CommandPermissionCondition(Operator.OR, this, allOr(conditions));
    }

    public CommandPermissionCondition not() {
        return new CommandPermissionCondition(Operator.NOT, this);
    }

    // --- Evaluate ---
    public <S extends Source> boolean has(S source, PermissionChecker<S> checker) {

        if (permission != null) {
            return checker.hasPermission(source, permission);
        }

        return switch (operator) {
            case AND -> children.stream().allMatch(child -> child.has(source, checker));
            case OR -> children.stream().anyMatch(child -> child.has(source, checker));
            case NOT -> !children.get(0).has(source, checker);
            default -> throw new IllegalStateException("Unknown operator");
        };
    }

    @Override
    public String toString() {
        if (permission != null) {
            return permission;
        }
        return switch (operator) {
            case AND -> "(" + children.stream().map(Object::toString).collect(Collectors.joining(" & ")) + ")";
            case OR -> "(" + children.stream().map(Object::toString).collect(Collectors.joining(" | ")) + ")";
            case NOT -> "(!" + children.get(0).toString() + ")";
            default -> "";
        };
    }

    /*private <S extends Source> boolean has0(CommandPermissionCondition permissionCondition, S source, PermissionChecker<S> checker) {
        boolean res =

    }*/

    /**
     * Recursively collects all permission strings used in this condition and its children.
     * @return a list of all permissions used in this condition tree.
     */
    public List<String> collectPermissionsUsedOnConditions() {
        List<String> perms = new ArrayList<>();

        if (this.permission != null && !this.permission.isEmpty()) {
            perms.add(this.permission);
        }

        for (CommandPermissionCondition child : children) {
            perms.addAll(child.collectPermissionsUsedOnConditions());
        }

        return perms;
    }

    private enum Operator {AND, OR, NOT}
}
