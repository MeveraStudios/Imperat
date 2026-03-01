package studio.mevera.imperat.bukkit.test.commands;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Permission;
import studio.mevera.imperat.annotations.types.RootCommand;

@RootCommand({"gamemode", "gm"})
@Permission("lobby.gamemode")
class GameModeCommand {

    @Execute
    public void defUsage(
            Player source,
            @NotNull @Named("gamemode") GameMode gameMode,
            @Default("me") @Named("player") Player target
    ) {
        // /gamemode <gamemode> [player]
        target.setGameMode(gameMode);
        target.sendMessage("Your gamemode has been set to " + gameMode.name());
        if (target != source) {
            source.sendMessage("You have set " + target.getName() + "'s gamemode to " + gameMode.name());
        }
    }

    @RootCommand("gmc")
    public void gmc(Player source, @Default("me") @Named("player") Player target) {
        defUsage(source, GameMode.CREATIVE, target);
    }

    @RootCommand("gms")
    public void gms(Player source, @Default("me") @Named("player") Player target) {
        defUsage(source, GameMode.SURVIVAL, target);
    }

    @RootCommand("gma")
    public void gma(Player source, @Default("me") @Named("player") Player target) {
        defUsage(source, GameMode.ADVENTURE, target);
    }

    @RootCommand("gmsp")
    public void gmsp(Player source, @Default("me") @Named("player") Player target) {
        defUsage(source, GameMode.SPECTATOR, target);
    }

}

