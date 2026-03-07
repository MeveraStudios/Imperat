package studio.mevera.imperat.tests.syntax.commands;

import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestCommandSource;

@RootCommand("usagetest")
public class UsageTestCommand {

    @SubCommand("simple")
    public void simpleCommand(
            TestCommandSource source,
            @Named("name") String name,
            @Named("age") int age
    ) {
        source.reply("Simple command executed: name=" + name + ", age=" + age);
    }

    @SubCommand("optional")
    public void optionalCommand(
            TestCommandSource source,
            @Named("required") String required
    ) {
        source.reply("Optional command executed: required=" + required);
    }

    @SubCommand("optional")
    public void optionalCommandWithOptional(
            TestCommandSource source,
            @Named("required") String required,
            @Named("optional") String optional
    ) {
        source.reply("Optional command executed: required=" + required + ", optional=" + optional);
    }

    @SubCommand("chain")
    public void chainCommand(
            TestCommandSource source,
            @Named("required") String required
    ) {
        source.reply("Chain command executed: required=" + required);
    }

    @SubCommand("chain")
    public void chainCommandOpt1(
            TestCommandSource source,
            @Named("required") String required,
            @Named("opt1") String opt1
    ) {
        source.reply("Chain command executed: required=" + required + ", opt1=" + opt1);
    }

    @SubCommand("chain")
    public void chainCommandOpt2(
            TestCommandSource source,
            @Named("required") String required,
            @Named("opt1") String opt1,
            @Named("opt2") String opt2
    ) {
        source.reply("Chain command executed: required=" + required + ", opt1=" + opt1 + ", opt2=" + opt2);
    }

    @SubCommand("chain")
    public void chainCommandOpt3(
            TestCommandSource source,
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
            TestCommandSource source,
            @Named("target") String target
    ) {
        source.reply("Flags command executed: target=" + target);
    }

    // ==================== MULTIPLE USAGE VARIATIONS ====================

    @SubCommand("nested")
    public static class NestedCommand {

        @SubCommand("sub1")
        public void sub1Command(
                TestCommandSource source,
                @Named("param1") String param1
        ) {
            source.reply("Nested sub1 executed: param1=" + param1);
        }

        @SubCommand("sub2")
        public void sub2Command(
                TestCommandSource source,
                @Named("param2") int param2
        ) {
            source.reply("Nested sub2 executed: param2=" + param2);
        }

        @SubCommand(value = "sub1")
        public static class Sub1Deep {

            @SubCommand("deep")
            public void deepCommand(
                    TestCommandSource source,
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
                TestCommandSource source,
                @Named("item") String item
        ) {
            source.reply("Added item: " + item);
        }

        @SubCommand("remove")
        public void removeItem(
                TestCommandSource source,
                @Named("item") String item
        ) {
            source.reply("Removed item: " + item);
        }

        @SubCommand("list")
        public void listItems(TestCommandSource source) {
            source.reply("Listing all items");
        }

        @SubCommand("clear")
        public void clearItems(TestCommandSource source) {
            source.reply("Cleared all items");
        }
    }

    // ==================== BFS TEST COMMAND ====================

    @SubCommand("bfs")
    public static class BFSTestCommand {

        @SubCommand("child1")
        public void child1(
                TestCommandSource source,
                @Named("arg1") String arg1
        ) {
            source.reply("Child1 executed: arg1=" + arg1);
        }

        @SubCommand("child2")
        public void child2Executable(TestCommandSource source) {
            source.reply("Child2 executed (no args)");
        }

        @SubCommand("child3")
        public void child3(
                TestCommandSource source,
                @Named("arg3") String arg3
        ) {
            source.reply("Child3 executed: arg3=" + arg3);
        }
    }
}