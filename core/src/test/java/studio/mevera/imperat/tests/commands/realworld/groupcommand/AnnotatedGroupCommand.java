package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.command.tree.help.HelpQuery;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.HelpTheme;
import studio.mevera.imperat.tests.TestSource;

@Command("group")
public final class AnnotatedGroupCommand {

    @Usage
    public void defaultUsage(TestSource source) {
        //default execution = no args
        source.reply("/group <group>");
    }

    @Usage
    @Description("Shows sub-commands.")
    public void mainUsage(
        TestSource source,
        @Named("group") Group group
    ) {
        //when he does "/group <group>"
        source.reply("entered group name= " + group.name());
    }

    @SubCommand(value = "help", attachment = AttachmentMode.DEFAULT)
    @Description("Shows Help for the command.")
    public void help(TestSource source, CommandHelp<TestSource> commandHelp) {
        // /group help
        commandHelp.display(
                HelpQuery.<TestSource>builder()
                        .limit(30)
                        .maxDepth(10)
                        .build(),
                
                HelpRenderOptions.<TestSource>builder()
                        .layout(HelpRenderOptions.Layout.TREE)
                        .theme(HelpTheme.defaultTheme())
        );
        
    }

    @SubCommand("setperm")
    @Description("Sets permission for a group.")
    public void setGroupPermission(TestSource source,
                              @Named("group") Group group,
                              @Named("permission") String permission) {
        // /group <group> setperm <permission>
        source.reply("You have set permission '" + permission
            + "' to group '" + group.name() + "'");
    }

    @SubCommand("setprefix")
    @Description("Sets prefix for a group.")
    public void setPrefix(
        TestSource source,
        @Named("group") Group group,
        @Named("prefix") String prefix
    ) {
        // /group <group> setprefix <prefix>
        source.reply("You have set prefix '" + prefix + "' to group '" + group.name() + "'");
    }

}