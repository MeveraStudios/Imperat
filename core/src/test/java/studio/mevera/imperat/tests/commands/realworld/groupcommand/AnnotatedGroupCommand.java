package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.annotations.types.Context;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.InheritedArg;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Shortcut;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("group")
public final class AnnotatedGroupCommand {

    @Execute
    public void defaultUsage(TestSource source, @Context CommandHelp<TestSource> commandHelp) {
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

    @SubCommand(value = "setperm", attachTo = "<group>")
    @Description("Sets permission for a group.")
    @Shortcut("setgroupperm")
    public void setGroupPermission(
            TestSource source,
            @InheritedArg @Named("group") Group group,
            @Named("permission") String permission) {
        // /group <group> setperm <permission>
        source.reply("You have set permission '" + permission
                             + "' to group '" + group.name() + "'");
    }

    @SubCommand(value = "setprefix", attachTo = "<group>")
    @Description("Sets prefix for a group.")
    @Shortcut("setgroupprefix")
    public void setPrefix(
            TestSource source,
            @InheritedArg @Named("group") Group group,
            @Named("prefix") String prefix
    ) {
        // /group <group> setprefix <prefix>
        source.reply("You have set prefix '" + prefix + "' to group '" + group.name() + "'");
    }

}