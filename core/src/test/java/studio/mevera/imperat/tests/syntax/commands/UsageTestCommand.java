package studio.mevera.imperat.tests.syntax.commands;

import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("usagetest")
public class UsageTestCommand {

    @SubCommand("simple")
    public void simpleCommand(
            TestSource source,
            @Named("name") String name,
            @Named("age") int age
    ) {
        source.reply("Simple command executed: name=" + name + ", age=" + age);
    }

    @SubCommand("optional")
    public void optionalCommand(
            TestSource source,
            @Named("required") String required
    ) {
        source.reply("Optional command executed: required=" + required);
    }

    @SubCommand("optional")
    public void optionalCommandWithOptional(
            TestSource source,
            @Named("required") String required,
            @Named("optional") String optional
    ) {
        source.reply("Optional command executed: required=" + required + ", optional=" + optional);
    }

    @SubCommand("chain")
    public void chainCommand(
            TestSource source,
            @Named("required") String required
    ) {
        source.reply("Chain command executed: required=" + required);
    }

    @SubCommand("chain")
    public void chainCommandOpt1(
            TestSource source,
            @Named("required") String required,
            @Named("opt1") String opt1
    ) {
        source.reply("Chain command executed: required=" + required + ", opt1=" + opt1);
    }

    @SubCommand("chain")
    public void chainCommandOpt2(
            TestSource source,
            @Named("required") String required,
            @Named("opt1") String opt1,
            @Named("opt2") String opt2
    ) {
        source.reply("Chain command executed: required=" + required + ", opt1=" + opt1 + ", opt2=" + opt2);
    }

    @SubCommand("chain")
    public void chainCommandOpt3(
            TestSource source,
            @Named("required") String required,
            @Named("opt1") String opt1,
            @Named("opt2") String opt2,
            @Named("opt3") String opt3
    ) {
        source.reply("Chain command executed: all params=" + required + ", " + opt1 + ", " + opt2 + ", " + opt3);
    }

    // ==================== NESTED SUBCOMMANDS ====================

    @SubCommand("flags")
    public void flagsCommand(
            TestSource source,
            @Named("target") String target
    ) {
        source.reply("Flags command executed: target=" + target);
    }

    // ==================== MULTIPLE USAGE VARIATIONS ====================

    @SubCommand("nested")
    public static class NestedCommand {

        @SubCommand("sub1")
        public void sub1Command(
                TestSource source,
                @Named("param1") String param1
        ) {
            source.reply("Nested sub1 executed: param1=" + param1);
        }

        @SubCommand("sub2")
        public void sub2Command(
                TestSource source,
                @Named("param2") int param2
        ) {
            source.reply("Nested sub2 executed: param2=" + param2);
        }

        @SubCommand(value = "sub1")
        public static class Sub1Deep {

            @SubCommand("deep")
            public void deepCommand(
                    TestSource source,
                    @Named("value") String value
            ) {
                source.reply("Deep nested command executed: value=" + value);
            }
        }
    }

    // ==================== COMMANDS WITH FLAGS ====================

    @SubCommand("multi")
    public static class MultiUsageCommand {

        @SubCommand("add")
        public void addItem(
                TestSource source,
                @Named("item") String item
        ) {
            source.reply("Added item: " + item);
        }

        @SubCommand("remove")
        public void removeItem(
                TestSource source,
                @Named("item") String item
        ) {
            source.reply("Removed item: " + item);
        }

        @SubCommand("list")
        public void listItems(TestSource source) {
            source.reply("Listing all items");
        }

        @SubCommand("clear")
        public void clearItems(TestSource source) {
            source.reply("Cleared all items");
        }
    }

    // ==================== BFS TEST COMMAND ====================

    @SubCommand("bfs")
    public static class BFSTestCommand {

        @SubCommand("child1")
        public void child1(
                TestSource source,
                @Named("arg1") String arg1
        ) {
            source.reply("Child1 executed: arg1=" + arg1);
        }

        @SubCommand("child2")
        public void child2Executable(TestSource source) {
            source.reply("Child2 executed (no args)");
        }

        @SubCommand("child3")
        public void child3(
                TestSource source,
                @Named("arg3") String arg3
        ) {
            source.reply("Child3 executed: arg3=" + arg3);
        }
    }
}