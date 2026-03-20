package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.PathwayCommand;
import studio.mevera.imperat.tests.TestCommandSource;

public class FlatRankCommand {

    @PathwayCommand("rank <rank> permission set <perm> [value]")
    public void setPermission(TestCommandSource source, String rank, String perm, @Default("true") boolean value) {
        System.out.println("Set permission " + perm + " to " + value + " for rank " + rank);
    }

    @PathwayCommand("rank <rank> permission unset <perm>")
    public void unsetPermission(TestCommandSource source, String rank, String perm) {
        System.out.println("Unset permission " + perm + " for rank " + rank);
    }

}
