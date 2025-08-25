package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.command.tree.help.*;
import studio.mevera.imperat.tests.TestSource;

@Command("group")
public final class AnnotatedGroupCommand {

    @Usage
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

    @Usage
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