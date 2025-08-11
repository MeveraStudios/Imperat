package studio.mevera.imperat.tests.commands.complex;

import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.tests.TestSource;

@Command("test")
@ExternalSubCommand(FirstSub.class)
public class TestCommand {


    @Usage
    public void defaultExec(TestSource source) {
        source.reply("Default execution of test(root) command");
    }

    @Usage
    public void cmdUsage(TestSource source, @Named("otherText") @Suggest({"hi", "bye"}) String otherText, @Named("otherText2") String otherText2) {
        source.reply("Executing usage in test's main usage, num= " + otherText);
    }

    @SubCommand("othersub")
    public void doOtherSub(TestSource source, @Named("text") @Suggest({"hi", "bye"}) String text) {
        source.reply("Other-text= " + text);
    }

    @SubCommand(value = "help", attachment = AttachmentMode.DEFAULT)
    public void help(TestSource source) {
        //help.display(source);
        source.reply("executed /test help");
    }

    @Command("embedded")
    public void embeddedCmd(TestSource source, @Named("value") String arg) {
        source.reply("Embedded command value=" + arg);
    }


    @SubCommand("sub1")
    public static class Sub1 {
        @Usage
        public void defaultUsage(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2) {
            source.reply("default sub1");
        }

        @Usage
        public void sub1Main(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2, @Named("a") String a) {
            source.reply("otherText=" + otherText + ", sub1-main a=" + a);
        }

        @SubCommand("sub2")
        public static class Sub2 {


            @Usage
            public void defaultUsage(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2, @Named("a") String a) {
                source.reply("default sub2");
            }

            @Usage
            public void sub2Main(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2, @Named("a") String a, @Named("b") String b) {
                source.reply("sub2-main b=" + b);
            }

            @SubCommand("sub3")
            public static class Sub3 {

                @Usage
                public void defaultUsage(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2, @Named("a") String a, @Named("b") String b) {
                    source.reply("default sub3");
                }

                @Usage
                public void sub3Main(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2, @Named("a") String a, @Named("b") String b, @Named("c") String c) {
                    source.reply("sub3 c=" + c);
                }

            }

        }
    }


    @SubCommand("sub4")
    public static class Sub4 {

        @Usage
        public void defaultUsage(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2) {
            source.reply("default sub4");
        }

        @Usage
        public void sub4Main(TestSource source, @Named("othertext") String otherText,@Named("otherText2") String otherText2, @Named("a") String a) {
            source.reply("sub4 a=" + a);
        }

        @SubCommand("sub5")
        public static class Sub5 {

            @Usage
            public void defaultUsage(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2, @Named("a") String a) {
                source.reply("default sub5");
            }

            @Usage
            public void sub5Main(TestSource source, @Named("othertext") String otherText, @Named("otherText2") String otherText2, @Named("a") String a, @Named("b") String b) {
                source.reply("sub4 a= " + a + ", sub5 b=" + b);
            }

            @SubCommand("sub6")
            public static class Sub6 {
                @Usage
                public void defaultUsage(TestSource source, @Named("othertext") String otherText, @Named("otherText2") String otherText2, @Named("a") String a, @Named("b") String b) {
                    source.reply("default sub6");
                }

                @Usage
                public void sub6Main(TestSource source, @Named("othertext") String otherText, @Named("otherText2") String otherText2, @Named("a") String a, @Named("b") String b, @Named("c") String c) {
                    source.reply("sub4 a= " + a + ", sub5b= " + b + ", sub6 c=" + c);
                }

            }

        }

    }

}