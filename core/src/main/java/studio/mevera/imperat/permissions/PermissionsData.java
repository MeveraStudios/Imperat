package studio.mevera.imperat.permissions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PermissionsData {

    private final List<String> permissions;
    private CommandPermissionCondition condition;

    private PermissionsData(List<String> permissions) {
        this.permissions = permissions;
        this.condition = CommandPermissionCondition.all(permissions);
    }

    private PermissionsData(CommandPermissionCondition condition) {
        this.permissions = condition.collectPermissionsUsedOnConditions();
        this.condition = condition;
    }

    private PermissionsData() {
        this.permissions = new ArrayList<>();
        this.condition = CommandPermissionCondition.empty();
    }

    public static PermissionsData of(String... permissions) {
        return new PermissionsData(Arrays.asList(permissions));
    }

    public static PermissionsData of(List<String> permissions) {
        return new PermissionsData(permissions);
    }

    public static PermissionsData of(CommandPermissionCondition condition) {
        return new PermissionsData(condition);
    }

    public static PermissionsData empty() {
        return new PermissionsData();
    }

    public static PermissionsData fromText(String permLine) {
        return of(CommandPermissionCondition.fromText(permLine));
    }

    public CommandPermissionCondition getCondition() {
        return condition;
    }

    public void setCondition(CommandPermissionCondition condition) {
        this.condition = condition;
    }

    public void addPermissions(String... permissions) {
        this.permissions.addAll(Arrays.asList(permissions));
        condition = condition.and(permissions);
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public boolean isEmpty() {
        return permissions.isEmpty();
    }

    public void append(@NotNull PermissionsData permissionsData) {
        this.permissions.addAll(permissionsData.permissions);
        this.condition = this.condition.and(permissionsData.condition);
    }
}
