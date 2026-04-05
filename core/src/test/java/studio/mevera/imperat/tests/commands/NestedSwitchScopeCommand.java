package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;

@RootCommand("switchscope")
public final class NestedSwitchScopeCommand {

    @Execute
    public void root(TestCommandSource source) {
        source.reply("root");
    }

    @SubCommand("sub1")
    public class Sub1 {

        @Execute
        public void sub1(TestCommandSource source, @Switch("sub1switch") boolean sub1switch) {
            source.reply("sub1=" + sub1switch);
        }

        @Execute
        public void sub1(TestCommandSource source, int number) {
            source.reply("number=" + number);
        }

        @SubCommand("sub2")
        public class Sub2 {

            @Execute
            public void sub2(TestCommandSource source, @Switch("sub2switch") boolean sub2switch) {
                source.reply("sub2=" + sub2switch);
            }

            @SubCommand("sub3")
            public class Sub3 {

                @Execute
                public void sub3(
                        TestCommandSource source,
                        TestPlayer target,
                        @Switch("sub3switch") boolean sub3switch
                ) {
                    source.reply("target=" + target);
                    source.reply("sub3=" + sub3switch);
                }
            }
        }
    }
}
