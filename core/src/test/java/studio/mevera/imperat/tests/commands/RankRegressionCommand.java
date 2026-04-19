package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Mirrors the structure that triggered the "wrong closest usage" and
 * "Invalid Integer Format 'setcolor'" bugs: a help subcommand declared
 * first (so it is the first root child tried) whose only trailing
 * parameter is an optional int, sitting next to a deeper subcommand
 * that requires two string arguments.
 */
@RootCommand("rankreg")
public class RankRegressionCommand {

    @Execute
    @SubCommand("help")
    public void help(
            final TestCommandSource actor,
            @Named("page") @Default("1") int page
    ) {
        actor.reply("help page=" + page);
    }

    @SubCommand("setcolor")
    public void setColor(
            final TestCommandSource actor,
            @Named("rank") String rank,
            @Named("color") String color
    ) {
        actor.reply("setcolor " + rank + " " + color);
    }
}
