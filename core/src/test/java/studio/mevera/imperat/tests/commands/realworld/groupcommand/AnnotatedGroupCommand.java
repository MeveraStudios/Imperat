package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.tests.TestSource;

@Command("group")
public final class AnnotatedGroupCommand {

    @Usage
    public void defaultUsage(TestSource source) {
        //default execution = no args
        source.reply("/group <group>");
    }

    @Usage
    public void mainUsage(
        TestSource source,
        @Named("group") Group group
    ) {
        //when he does "/group <group>"
        source.reply("entered group name= " + group.name());
    }

    @SubCommand(value = "help", attachment = AttachmentMode.DEFAULT)
    public void help(TestSource source) {
        // /group help
        //help.display(source);
    }

    @SubCommand("setperm")
    public void setGroupPermission(TestSource source,
                              @Named("group") Group group,
                              @Named("permission") String permission) {
        // /group <group> setperm <permission>
        source.reply("You have set permission '" + permission
            + "' to group '" + group.name() + "'");
    }

    @SubCommand("setprefix")
    public void setPrefix(
        TestSource source,
        @Named("group") Group group,
        @Named("prefix") String prefix
    ) {
        // /group <group> setprefix <prefix>
        source.reply("You have set prefix '" + prefix + "' to group '" + group.name() + "'");
    }

}