package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Shortcut;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.tests.TestSource;

@Command("group")
public final class AnnotatedGroupCommand {

    @Execute
    public void defaultUsage(TestSource source, CommandHelp<TestSource> commandHelp) {
        //default execution = no args
        // /group help
        /*commandHelp.display(
                HelpQuery.<TestSource>builder()
                        .filter(HelpFilters.hasPermission(source, commandHelp.getContext()))
                        .build(),
                
                HelpRenderOptions.of(
                
                )
        );*/
    }

    @Execute
    @Description("Shows sub-commands.")
    public void mainUsage(
            TestSource source,
            @Named("group") Group group
    ) {
        //when he does "/group <group>"
        source.reply("entered group name= " + group.name());
    }

    @SubCommand("setperm")
    @Description("Sets permission for a group.")
    @Shortcut("setgroupperm")
    public void setGroupPermission(TestSource source,
            @Named("group") Group group,
            @Named("permission") String permission) {
        // /group <group> setperm <permission>
        source.reply("You have set permission '" + permission
                             + "' to group '" + group.name() + "'");
    }

    @SubCommand("setprefix")
    @Description("Sets prefix for a group.")
    @Shortcut("setgroupprefix")
    public void setPrefix(
            TestSource source,
            @Named("group") Group group,
            @Named("prefix") String prefix
    ) {
        // /group <group> setprefix <prefix>
        source.reply("You have set prefix '" + prefix + "' to group '" + group.name() + "'");
    }

}