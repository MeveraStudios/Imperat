package studio.mevera.imperat.selector.field.filters;

import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Arrays;

final class GamemodeField extends PredicateField<GameMode> {

    GamemodeField(String name) {
        super(name, TypeWrap.of(GameMode.class));
        Arrays.stream(GameMode.values())
            .map(GameMode::name)
            .map(String::toLowerCase)
            .forEach(suggestions::add);
    }

    @Override
    protected @NotNull EntityCondition getCondition(GameMode value, CommandInputStream<BukkitSource> commandInputStream, Context<BukkitSource> context) {
        return ((sender, entity) -> {
            if (!(entity instanceof HumanEntity humanEntity)) return false;
            return humanEntity.getGameMode() == value;
        });
    }

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value   the string representation of the value to be parsed
     * @param context
     * @return the parsed value of the field's type
     * @throws CommandException if the parsing fails
     */
    @Override
    public GameMode parseFieldValue(String value, Context<BukkitSource> context) throws CommandException {
        try {
            return GameMode.valueOf(value);
        } catch (EnumConstantNotPresentException ex) {
            throw new SourceException("Unknown gamemode '%s'", value);
        }
    }
}
