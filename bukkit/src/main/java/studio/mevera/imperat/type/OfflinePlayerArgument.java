package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.resolvers.SuggestionResolver;

import java.util.Arrays;
import java.util.List;

public class OfflinePlayerArgument extends ArgumentType<BukkitSource, OfflinePlayer> {


    private final PlayerSuggestionResolver playerSuggestionResolver = new PlayerSuggestionResolver();

    public OfflinePlayerArgument() {
        super();
    }

    @Override
    public @Nullable OfflinePlayer resolve(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull Cursor<BukkitSource> cursor,
            @NotNull String correspondingInput) throws CommandException {

        if (correspondingInput.length() > 16) {
            throw new UnknownPlayerException(correspondingInput);
        }

        return Bukkit.getOfflinePlayer(correspondingInput);
    }

    @Override
    public SuggestionResolver<BukkitSource> getSuggestionResolver() {
        return playerSuggestionResolver;
    }

    private final static class PlayerSuggestionResolver implements SuggestionResolver<BukkitSource> {

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> autoComplete(SuggestionContext<BukkitSource> context, Argument<BukkitSource> parameter) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                           .map(OfflinePlayer::getName)
                           .toList();
        }
    }
}
