package studio.mevera.imperat.tests.commands.complex;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.ExternalSubCommand;
import studio.mevera.imperat.annotations.types.InheritedArg;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Suggest;
import studio.mevera.imperat.tests.TestCommandSource;

@RootCommand("test")
@ExternalSubCommand(FirstSub.class)
public class TestCommand {


    @Execute
    public void defaultExec(TestCommandSource source) {
        source.reply("Default execution of test(root) command");
    }

    @Execute
    public void cmdUsage(TestCommandSource source, @Named("otherText") @Suggest({"hi", "bye"}) String otherText,
            @Named("otherText2") String otherText2) {
        source.reply("Executing usage in test's main usage, num= " + otherText);
    }

    @SubCommand("othersub")
    public void doOtherSub(TestCommandSource source, @Named("text") @Suggest({"hi", "bye"}) String text) {
        source.reply("Other-text= " + text);
    }

    @SubCommand(value = "help")
    public void help(TestCommandSource source) {
        // help.show();
        source.reply("executed /test help");
    }

    @RootCommand("embedded")
    public void embeddedCmd(TestCommandSource source, @Named("value") String arg) {
        source.reply("Embedded command value=" + arg);
    }


    @SubCommand(value = "sub1", attachTo = "<otherText2>")
    public static class Sub1 {

        @Execute
        public void defaultUsage(
                TestCommandSource source,
                @InheritedArg @Named("otherText") String otherText,
                @InheritedArg @Named("otherText2") String otherText2
        ) {
            source.reply("default sub1");
        }

        @Execute
        public void sub1Main(
                TestCommandSource source,
                @InheritedArg @Named("otherText") String otherText,
                @InheritedArg @Named("otherText2") String otherText2,
                @Named("a") String a
        ) {
            source.reply("otherText=" + otherText + ", sub1-main a=" + a);
        }

        @SubCommand(value = "sub2", attachTo = "<a>")
        public static class Sub2 {


            @Execute
            public void defaultUsage(TestCommandSource source, @InheritedArg @Named("otherText") String otherText,
                    @InheritedArg @Named("otherText2") String otherText2,
                    @InheritedArg @Named("a") String a) {
                source.reply("default sub2");
            }

            @Execute
            public void sub2Main(TestCommandSource source, @InheritedArg @Named("otherText") String otherText,
                    @InheritedArg @Named("otherText2") String otherText2,
                    @InheritedArg @Named("a") String a, @Named("b") String b) {
                source.reply("sub2-main b=" + b);
            }

            @SubCommand(value = "sub3", attachTo = "<b>")
            public static class Sub3 {

                @Execute
                public void defaultUsage(TestCommandSource source, @InheritedArg @Named("otherText") String otherText,
                        @InheritedArg @Named("otherText2") String otherText2,
                        @InheritedArg @Named("a") String a, @InheritedArg @Named("b") String b) {
                    source.reply("default sub3");
                }

                @Execute
                public void sub3Main(TestCommandSource source,
                        @InheritedArg @Named("otherText") String otherText,
                        @InheritedArg @Named("otherText2") String otherText2,
                        @InheritedArg @Named("a") String a,
                        @InheritedArg @Named("b") String b,
                        @Named("c") String c) {
                    source.reply("sub3 c=" + c);
                }

            }

        }
    }


    @SubCommand(value = "sub4", attachTo = "<otherText2>")
    public static class Sub4 {

        @Execute
        public void defaultUsage(TestCommandSource source,
                @InheritedArg @Named("otherText") String otherText,
                @InheritedArg @Named("otherText2") String otherText2
        ) {
            source.reply("default sub4");
        }

        @Execute
        public void sub4Main(TestCommandSource source,
                @InheritedArg @Named("othertext") String otherText,
                @InheritedArg @Named("otherText2") String otherText2,
                @Named("a") String a
        ) {
            source.reply("sub4 a=" + a);
        }

        @SubCommand(value = "sub5", attachTo = "<a>")
        public static class Sub5 {

            @Execute
            public void defaultUsage(TestCommandSource source,
                    @InheritedArg @Named("otherText") String otherText,
                    @InheritedArg @Named("otherText2") String otherText2,
                    @InheritedArg @Named("a") String a) {
                source.reply("default sub5");
            }

            @Execute
            public void sub5Main(TestCommandSource source,
                    @InheritedArg @Named("othertext") String otherText,
                    @InheritedArg @Named("otherText2") String otherText2,
                    @InheritedArg @Named("a") String a, @Named("b") String b) {
                source.reply("sub4 a= " + a + ", sub5 b=" + b);
            }

            @SubCommand(value = "sub6", attachTo = "<b>")
            public static class Sub6 {

                @Execute
                public void defaultUsage(TestCommandSource source,

                        @InheritedArg @Named("othertext") String otherText,
                        @InheritedArg @Named("otherText2") String otherText2,
                        @InheritedArg @Named("a") String a,
                        @InheritedArg @Named("b") String b) {
                    source.reply("default sub6");
                }

                @Execute
                public void sub6Main(
                        TestCommandSource source,
                        @InheritedArg @Named("othertext") String otherText,
                        @InheritedArg @Named("otherText2") String otherText2,
                        @InheritedArg @Named("a") String a,
                        @InheritedArg @Named("b") String b,
                        @Named("c") String c
                ) {
                    source.reply("sub4 a= " + a + ", sub5b= " + b + ", sub6 c=" + c);
                }

            }

        }

    }

}
