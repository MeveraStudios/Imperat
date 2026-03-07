package studio.mevera.imperat.tests.commands.realworld.greedy;

import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestCommandSource;

import java.util.Collection;
import java.util.stream.Collectors;

@RootCommand("lg")
public class LimitedGenericGreedy {

    @SubCommand("array")
    public void exec(TestCommandSource src, @Greedy(limit = 3) String[] args, int num) {
        src.reply("args=" + String.join("|", args));
        src.reply("num=" + num);
    }

    @SubCommand("collection")
    public void exec2(TestCommandSource src, @Greedy(limit = 3) Collection<String> args, int num) {
        src.reply("args=" + String.join("|", args));
        src.reply("num=" + num);
    }

    //map
    @SubCommand("map")
    public void exec3(TestCommandSource src, @Greedy(limit = 3) java.util.Map<String, String> args, int num) {
        src.reply("args=" + args.entrySet()
                                    .stream().map(e -> e.getKey() + "=" + e.getValue())
                                    .collect(Collectors.joining(",")));
        src.reply("num=" + num);
    }

}
