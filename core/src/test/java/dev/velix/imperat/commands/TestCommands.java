package dev.velix.imperat.commands;

import dev.velix.imperat.TestRun;
import dev.velix.imperat.command.AttachmentMode;
import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.components.TestSource;
import dev.velix.imperat.help.CommandHelp;

@SuppressWarnings("unused")
public final class TestCommands {

    public final static Command<TestSource> GROUP_CMD = Command.create(TestRun.IMPERAT, "group")
        .defaultExecution((source, context) -> {
            source.reply("/group <group>");
        })
        .usage(CommandUsage.<TestSource>builder()
            .parameters(CommandParameter.requiredText("group"))
            .execute((source, context) -> {
                source.reply("Executing /group " + context.getArgument("group") + " without any other args");
            })

        )
        .subCommand(
            Command.create(TestRun.IMPERAT, "setperm")
                .usage(CommandUsage.<TestSource>builder()
                    .parameters(
                        CommandParameter.requiredText("permission"),
                        CommandParameter.<TestSource>optionalBoolean("value").defaultValue(false)
                    )
                    .execute((source, ctx) -> {
                        TestRun.USAGE_EXECUTED = true;
                        String group = ctx.getArgument("group");
                        String permission = ctx.getArgument("permission");
                        Boolean value = ctx.getArgument("value");
                        source.reply("Executing /group " + group + " setperm " + permission + " " + value);
                    })

                )
                .build()
        )
        .subCommand(Command.create(TestRun.IMPERAT, "setprefix")
            .usage(
                CommandUsage.<TestSource>builder()
                    .parameters(
                        CommandParameter.requiredText("prefix")
                    )
                    .execute((source, ctx) -> {
                        TestRun.USAGE_EXECUTED = true;
                        String group = ctx.getArgument("group");
                        String prefix = ctx.getArgument("prefix");
                        source.reply("Executing /group " + group + " setprefix " + prefix);
                    })
            )
            .build()
        )
        .subCommand(Command.create(TestRun.IMPERAT, "help")
            .usage(
                CommandUsage.<TestSource>builder()
                    .parameters(
                        CommandParameter.<TestSource>optionalInt("page").defaultValue(1)
                    )
                    .execute((source, context) -> {
                        Integer page = context.getArgument("page");

                        CommandHelp help = context.getContextResolvedArgument(CommandHelp.class);
                        assert help != null;
                        help.display(source);

                        source.sendMsg("Help page= " + page);
                        TestRun.USAGE_EXECUTED = true;
                    })

            ).build(), AttachmentMode.MAIN
        )
        .build();

    public final static Command<TestSource> CHAINED_SUBCOMMANDS_CMD =
        Command.create(TestRun.IMPERAT, "subs")
            .subCommand(
                Command.create(TestRun.IMPERAT, "first")
                    .defaultExecution((source, context) -> {
                        source.reply("FIRST, DEF EXEC");
                    })
                    .usage(CommandUsage.<TestSource>builder()
                        .parameters(CommandParameter.requiredText("arg1"))
                        .execute((source, context) -> source.reply("Arg1= " + context.getArgument("arg1")))

                    )
                    .subCommand(
                        Command.create(TestRun.IMPERAT, "second")
                            .defaultExecution((source, context) -> source.reply("SECOND, DEF EXEC"))
                            .usage(CommandUsage.<TestSource>builder()
                                .parameters(CommandParameter.requiredText("arg2"))
                                .execute((source, ctx) -> source.reply("Arg1= " + ctx.getArgument("arg1") + ", Arg2= " + ctx.getArgument("arg2")))
                            )
                            .subCommand(
                                Command.create(TestRun.IMPERAT, "third")
                                    .defaultExecution((source, context) -> source.reply("THIRD, DEF EXEC"))
                                    .usage(CommandUsage.<TestSource>builder()
                                        .parameters(CommandParameter.requiredText("arg3"))
                                        .execute((source, ctx) -> source.reply("Arg1= " + ctx.getArgument("arg1") + ", " +
                                            "Arg2= " + ctx.getArgument("arg2") + ", Arg3= " + ctx.getArgument("arg3")))
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )

            .build();

    public final static Command<TestSource> MULTIPLE_OPTIONAL_CMD =
        Command.create(TestRun.IMPERAT, "ot")
            .usage(
                CommandUsage.<TestSource>builder()
                    .parameters(
                        CommandParameter.requiredText("r1"),
                        CommandParameter.optionalText("o1"),
                        CommandParameter.requiredText("r2"),
                        CommandParameter.optionalText("o2")
                    )
                        .execute((src, ctx)-> {
                            
                            var r1 = ctx.getArgument("r1");
                            var o1 = ctx.getArgument("o1");
                            var r2 = ctx.getArgument("r2");
                            var o2 = ctx.getArgument("o2");
                            
                            System.out.println("R1= " + r1);
                            System.out.println("O1= " + o1);
                            System.out.println("R2= " + r2);
                            System.out.println("O2= " + o2);
                        })
            )
            .build();


    public final static Command<TestSource> BAN_COMMAND = Command.create(TestRun.IMPERAT, "ban")
            .permission("command.ban")
            .description("Main command for banning players")
            .usage(
                CommandUsage.<TestSource>builder()
                    .parameters(
                        CommandParameter.requiredText("player"),
                        CommandParameter.<TestSource>flagSwitch("silent").aliases("s"),
                        CommandParameter.optionalText("duration"),
                        CommandParameter.<TestSource>optionalGreedy("reason").defaultValue("Breaking server laws")
                    )
                    .execute((source, context)-> {
                        //getting arguments' values:
                        String player = context.getArgument("player");
                        String duration = context.getArgument("duration"); //may be null since we inserted it as optional
                        String reason = context.getArgument("reason");

                        //getting silent flag value, (false if the sender doesn't add '-s' or '-silent')
                        Boolean silent = context.getFlagValue("silent");
                        assert silent != null;

                        //TODO actual ban logic
                        String durationFormat = duration == null ? "FOREVER" : "for " + duration;
                        String msg = "Banning " + player + " " + durationFormat + " due to '" + reason + "'";
                        if (!silent)
                            source.reply("NOT SILENT= " + msg);
                        else
                            source.reply("SILENT= " + msg);
                    })
            )
            .build();
}
