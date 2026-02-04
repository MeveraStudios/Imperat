package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.tests.TestSource;

@Command({"party", "p"})
@Permission("voxy.party")
public class PartyCommand {

    @Usage
    @SubCommand(value = "help", attachment = AttachmentMode.EMPTY)
    @Description("Sends a help message")
    public void help(
            final TestSource player,
            final @Named("page") @Default("1") int page
    ) {
    }

    @SubCommand(value = "list", attachment = AttachmentMode.EMPTY)
    @Description("List of all players in your party")
    public void list(final TestSource player) {
    }

    @SubCommand(value = "invite", attachment = AttachmentMode.EMPTY)
    @Description("Invites a player to your party")
    public void invite(final TestSource sender, @Named("receiver") final String receiver) {

    }

    @SubCommand(value = "accept", attachment = AttachmentMode.EMPTY)
    @Description("Accepts a party invite")
    public void accept(final TestSource receiver, @Named("sender") final String sender) {
    }

    @SubCommand(value = "deny", attachment = AttachmentMode.EMPTY)
    @Description("Denies a party invite")
    public void deny(final TestSource receiver, @Named("sender") final String sender) {
    }

    @SubCommand(value = "leave", attachment = AttachmentMode.EMPTY)
    @Description("Leave your current party")
    public void leave(final TestSource player) {
    }

    @SubCommand(value = "disband", attachment = AttachmentMode.EMPTY)
    @Description("Disband a party")
    public void disband(final TestSource owner) {
    }
}