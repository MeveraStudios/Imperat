package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.PathwayCommand;
import studio.mevera.imperat.tests.TestCommandSource;

public class FlatRankCommand {

    @PathwayCommand("rank <rank> permission set <perm> [value]")
    public void setPermission(TestCommandSource source, String rank, String perm, @Default("true") boolean value) {
        // set a permission for your rank
    }

    @PathwayCommand("rank <rank> permission unset <perm>")
    public void unsetPermission(TestCommandSource source, String rank, String perm) {
        // unset a permission from your rank
    }

    @PathwayCommand("rank <rank> permission list")
    public void listRankPermissions(TestCommandSource source, String rank) {
        // list your rank permissions
    }

    @PathwayCommand("rank <rank> permission clear")
    public void clearPermissions(TestCommandSource source, String rank) {
        // clear all permissions of a rank.
    }

}
