package studio.mevera.imperat.bukkit.test.commands;

import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;

@RootCommand({"test", "tst"})
public class TestCmd {

    @SubCommand({"sub1", "s1", "subone"})
    public class SubCmd1 {

        @Execute
        public void sub1(BukkitCommandSource source) {
            // do something
        }

        @SubCommand({"sub2", "s2", "subtwo"})
        public void sub2(BukkitCommandSource source) {

        }
    }


}
