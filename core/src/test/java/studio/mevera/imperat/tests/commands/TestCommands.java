package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestSource;

@SuppressWarnings("unused")
public final class TestCommands {

    public final static Command<TestSource> GROUP_CMD = Command.create(ImperatTestGlobals.IMPERAT, "group")
                                                                .defaultExecution((source, context) -> {
                                                                    source.reply("/group <group>");
                                                                })
                                                                .usage(CommandPathway.<TestSource>builder()
                                                                               .parameters(Argument.requiredText("group"))
                                                                               .execute((source, context) -> {
                                                                                   source.reply("Executing /group " + context.getArgument("group")
                                                                                                        + " without any other args");
                                                                               })

                                                                )
                                                                .subCommand(
                                                                        Command.create(ImperatTestGlobals.IMPERAT, "setperm")
                                                                                .usage(CommandPathway.<TestSource>builder()
                                                                                               .parameters(
                                                                                                       Argument.requiredText("permission"),
                                                                                                       Argument.<TestSource>optionalBoolean(
                                                                                                               "value").defaultValue(false)
                                                                                               )
                                                                                               .execute((source, ctx) -> {
                                                                                                   String group = ctx.getArgument("group");
                                                                                                   String permission = ctx.getArgument("permission");
                                                                                                   Boolean value = ctx.getArgument("value");
                                                                                                   source.reply(
                                                                                                           "Executing /group " + group + " setperm "
                                                                                                                   + permission + " " + value);
                                                                                               })

                                                                                )
                                                                                .build()
                                                                )
                                                                .subCommand(Command.create(ImperatTestGlobals.IMPERAT, "setprefix")
                                                                                    .usage(
                                                                                            CommandPathway.<TestSource>builder()
                                                                                                    .parameters(
                                                                                                            Argument.requiredText("prefix")
                                                                                                    )
                                                                                                    .execute((source, ctx) -> {
                                                                                                        String group = ctx.getArgument("group");
                                                                                                        String prefix = ctx.getArgument("prefix");
                                                                                                        source.reply("Executing /group " + group
                                                                                                                             + " setprefix "
                                                                                                                             + prefix);
                                                                                                    })
                                                                                    )
                                                                                    .build()
                                                                )
                                                                .subCommand(Command.create(ImperatTestGlobals.IMPERAT, "help")
                                                                                    .usage(
                                                                                            CommandPathway.<TestSource>builder()
                                                                                                    .parameters(
                                                                                                            Argument.<TestSource>optionalInt(
                                                                                                                    "page").defaultValue(1)
                                                                                                    )
                                                                                                    .execute((source, context) -> {
                                                                                                        Integer page = context.getArgument("page");

                                                                                                        //CommandHelp help = context
                                                                                                        // .getContextResolvedArgument(CommandHelp
                                                                                                        // .class);
                                                                                                        //assert help != null;
                                                                                                        //help.display(source);

                                                                                                        source.sendMsg("Help page= " + page);
                                                                                                    })

                                                                                    ).build()
                                                                )
                                                                .build();

    public final static Command<TestSource> CHAINED_SUBCOMMANDS_CMD =
            Command.create(ImperatTestGlobals.IMPERAT, "subs")
                    .subCommand(
                            Command.create(ImperatTestGlobals.IMPERAT, "first")
                                    .defaultExecution((source, context) -> {
                                        source.reply("FIRST, DEF EXEC");
                                    })
                                    .usage(CommandPathway.<TestSource>builder()
                                                   .parameters(Argument.requiredText("arg1"))
                                                   .execute((source, context) -> source.reply("Arg1= " + context.getArgument("arg1")))

                                    )
                                    .subCommand(
                                            Command.create(ImperatTestGlobals.IMPERAT, "second")
                                                    .defaultExecution((source, context) -> source.reply("SECOND, DEF EXEC"))
                                                    .usage(CommandPathway.<TestSource>builder()
                                                                   .parameters(Argument.requiredText("arg2"))
                                                                   .execute((source, ctx) -> source.reply(
                                                                           "Arg1= " + ctx.getArgument("arg1") + ", Arg2= " + ctx.getArgument("arg2")))
                                                    )
                                                    .subCommand(
                                                            Command.create(ImperatTestGlobals.IMPERAT, "third")
                                                                    .defaultExecution((source, context) -> source.reply("THIRD, DEF EXEC"))
                                                                    .usage(CommandPathway.<TestSource>builder()
                                                                                   .parameters(Argument.requiredText("arg3"))
                                                                                   .execute((source, ctx) -> source.reply(
                                                                                           "Arg1= " + ctx.getArgument("arg1") + ", " +
                                                                                                   "Arg2= " + ctx.getArgument("arg2") + ", Arg3= "
                                                                                                   + ctx.getArgument("arg3")))
                                                                    )
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )

                    .build();

    public final static Command<TestSource> MULTIPLE_OPTIONAL_CMD =
            Command.create(ImperatTestGlobals.IMPERAT, "ot")
                    .usage(
                            CommandPathway.<TestSource>builder()
                                    .parameters(
                                            Argument.requiredText("r1"),
                                            Argument.optionalText("o1"),
                                            Argument.requiredText("r2"),
                                            Argument.optionalText("o2")
                                    )

                    )
                    .build();


    public final static Command<TestSource> BAN_COMMAND = Command.create(ImperatTestGlobals.IMPERAT, "ban")
                                                                  .permission(
                                                                          PermissionsData.of("command.ban")
                                                                  )
                                                                  .description("Main command for banning players")
                                                                  .usage(
                                                                          CommandPathway.<TestSource>builder()
                                                                                  .parameters(
                                                                                          Argument.requiredText("player"),
                                                                                          Argument.<TestSource>flagSwitch("silent")
                                                                                                  .aliases("s"),
                                                                                          Argument.optionalText("duration"),
                                                                                          Argument.<TestSource>optionalGreedy("reason")
                                                                                                  .defaultValue("Breaking server laws")
                                                                                  )
                                                                                  .execute((source, context) -> {
                                                                                      //getting arguments' values:
                                                                                      String player = context.getArgument("player");
                                                                                      String duration = context.getArgument(
                                                                                              "duration"); //may be null since we inserted it as
                                                                                      // optional
                                                                                      String reason = context.getArgument("reason");

                                                                                      //getting silent flag value, (false if the sender doesn't add
                                                                                      // '-s' or '-silent')
                                                                                      Boolean silent = context.getFlagValue("silent");
                                                                                      assert silent != null;

                                                                                      //TODO actual ban logic
                                                                                      String durationFormat =
                                                                                              duration == null ? "FOREVER" : "for " + duration;
                                                                                      String msg =
                                                                                              "Banning " + player + " " + durationFormat + " due to '"
                                                                                                      + reason + "'";
                                                                                      if (!silent) {
                                                                                          source.reply("NOT SILENT= " + msg);
                                                                                      } else {
                                                                                          source.reply("SILENT= " + msg);
                                                                                      }
                                                                                  })
                                                                  )
                                                                  .build();
}