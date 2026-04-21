package studio.mevera.imperat.bukkit.test.commands;

import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Suggest;

@RootCommand("flagtest")
public final class FlagBrigadierCmd {

    @Execute
    public void root(
            BukkitCommandSource source,
            @Flag({"scenario", "sc"})
            @Suggest({"kindergarten", "castle", "sandstorm", "tsunami"})
            String scenario
    ) {
    }

    @SubCommand("play")
    public void play(
            BukkitCommandSource source,
            @Flag({"scenario", "sc"})
            @Suggest({"kindergarten", "castle", "sandstorm", "tsunami"})
            String scenario
    ) {
    }

    @SubCommand("mix")
    public void mix(
            BukkitCommandSource source,
            String target,
            @Flag({"scenario", "sc"})
            @Suggest({"kindergarten", "castle", "sandstorm", "tsunami"})
            String scenario
    ) {
    }
}
