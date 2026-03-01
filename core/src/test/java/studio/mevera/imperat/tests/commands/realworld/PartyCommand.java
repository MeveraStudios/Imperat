package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Shortcut;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand({"party", "p"})
//@Permission("voxy.party")
public class PartyCommand {

    @Execute
    @SubCommand(value = "help")
    @Description("Sends a help message")
    public void help(
            final TestSource player,
            final @Named("page") @Default("1") int page
    ) {
    }

    @SubCommand(value = "list")
    @Description("List of all players in your party")
    public void list(final TestSource player) {
    }

    @SubCommand(value = "invite")
    @Description("Invites a player to your party")
    @Shortcut("pinvite")
    public void invite(final TestSource sender, @Named("receiver") final String receiver) {
        System.out.println("Inviting " + receiver + " to " + sender.name() + "'s party");
    }

    @SubCommand(value = "accept")
    @Description("Accepts a party invite")
    public void accept(final TestSource receiver, @Named("sender") final String sender) {
    }

    @SubCommand(value = "deny")
    @Description("Denies a party invite")
    public void deny(final TestSource receiver, @Named("sender") final String sender) {
    }

    @SubCommand(value = "leave")
    @Description("Leave your current party")
    public void leave(final TestSource player) {
    }

    @SubCommand(value = "disband")
    @Description("Disband a party")
    public void disband(final TestSource owner) {
    }
}