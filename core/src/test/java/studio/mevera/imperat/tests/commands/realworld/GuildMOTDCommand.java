package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.parameters.JavaDurationParser;

import java.time.Duration;

@RootCommand("motd")
public class GuildMOTDCommand {

    @Execute
    public void def(TestCommandSource source) {
        source.reply("Default motd execution");
    }

    @Execute
    public void mainUsage(
            TestCommandSource source,
            @Flag("time") @Default("24h") Duration time,
            @Greedy String message
    ) {
        // /motd [-time <value>] <message...>
        source.reply("Message: '" + message + "'");
        source.reply("Duration: '" + JavaDurationParser.formatDuration(time) + "'");
    }
}