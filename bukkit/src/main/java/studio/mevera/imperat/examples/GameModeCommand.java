package studio.mevera.imperat.examples;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Execute;

@Command({"gamemode", "gm"})
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

    @Command("gmc")
    public void gmc(Player source, @Default("me") @Named("player") Player target) {
        defUsage(source, GameMode.CREATIVE, target);
    }

    @Command("gms")
    public void gms(Player source, @Default("me") @Named("player") Player target) {
        defUsage(source, GameMode.SURVIVAL, target);
    }

    @Command("gma")
    public void gma(Player source, @Default("me") @Named("player") Player target) {
        defUsage(source, GameMode.ADVENTURE, target);
    }

    @Command("gmsp")
    public void gmsp(Player source, @Default("me") @Named("player") Player target) {
        defUsage(source, GameMode.SPECTATOR, target);
    }

}

