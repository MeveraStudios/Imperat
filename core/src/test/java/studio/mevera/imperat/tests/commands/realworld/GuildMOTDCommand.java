package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.parameters.JavaDurationParser;

import java.time.Duration;

@Command("motd")
public class GuildMOTDCommand {

    @Usage
    public void def(TestSource source) {
        source.reply("Default motd execution");
    }

    /*@Usage
    public void mainUsage(TestSource source, @Named("message") String msg, @Named("duration") @Default("24h")Duration duration) {
        source.reply("Message: '" + msg + "'");
        source.reply("Duration: '" + DurationParser.formatDuration(duration) + "'");
    }*/

    @Usage
    public void mainUsage(
            TestSource source,
            @Flag("time") @Default("24h") Duration time,
            @Named("message") @Greedy String message
    ) {
        // /motd [-time <value>] <message...>
        source.reply("Message: '" + message + "'");
        source.reply("Duration: '" + JavaDurationParser.formatDuration(time) + "'");
    }
}