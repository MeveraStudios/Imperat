package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.BukkitUtil;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.BukkitResponseKey;

import java.util.List;

public class PlayerArgument extends ArgumentType<BukkitSource, Player> {

    private final PlayerSuggestionProvider SUGGESTION_RESOLVER = new PlayerSuggestionProvider();
    private final DefaultValueProvider DEFAULT_VALUE_SUPPLIER = DefaultValueProvider.of("~");

    public PlayerArgument() {
        super();
    }

    @Override
    public @Nullable Player parse(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull Cursor<BukkitSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {

        if (correspondingInput.equalsIgnoreCase("me") || correspondingInput.equalsIgnoreCase("~")) {
            if (context.source().isConsole()) {
                throw new CommandException(BukkitResponseKey.UNKNOWN_PLAYER)
                              .withPlaceholder("input", correspondingInput);
            }
            return context.source().asPlayer();
        }

        final Player player = Bukkit.getPlayerExact(correspondingInput);
        if (player != null) {
            return player;
        }

        throw new CommandException(BukkitResponseKey.UNKNOWN_PLAYER)
                      .withPlaceholder("input", correspondingInput);
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionProvider<BukkitSource> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    /**
     * Returns the default value supplier for the given source and command parameter.
     * By default, this returns an empty supplier, indicating no default value.
     *
     * @return an {@link DefaultValueProvider} providing the default value, or empty if none.
     */
    @Override
    public DefaultValueProvider getDefaultValueProvider() {
        return DEFAULT_VALUE_SUPPLIER;
    }

    @Override
    public boolean matchesInput(int rawPosition, CommandContext<BukkitSource> context, Argument<BukkitSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        return BukkitUtil.PLAYER_USERNAME_PATTERN.matcher(input).matches()
                       && Bukkit.getPlayer(input) != null;
    }

    private final static class PlayerSuggestionProvider implements SuggestionProvider<BukkitSource> {

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<BukkitSource> context, Argument<BukkitSource> parameter) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
    }
}
