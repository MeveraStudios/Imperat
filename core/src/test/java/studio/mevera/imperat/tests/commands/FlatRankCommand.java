package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.PathwayCommand;
import studio.mevera.imperat.tests.TestCommandSource;

public class FlatRankCommand {

    @PathwayCommand("rank <rank> permission set <permission> [value]")
    public void setPermission(TestCommandSource source, String rank, String permission, @Default("false") boolean value) {

    }

}
