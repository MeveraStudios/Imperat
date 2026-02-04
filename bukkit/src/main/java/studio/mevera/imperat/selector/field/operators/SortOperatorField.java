package studio.mevera.imperat.selector.field.operators;

import org.bukkit.entity.Entity;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.util.TypeWrap;

import java.util.Arrays;
import java.util.List;

final class SortOperatorField extends OperatorField<SortOption> {

    SortOperatorField(String name) {
        super(name, TypeWrap.of(SortOption.class));
        Arrays.stream(SortOption.values())
                .map(SortOption::name)
                .map(String::toLowerCase)
                .forEach(suggestions::add);
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
    public SortOption parseFieldValue(String value, Context<BukkitSource> context) throws CommandException {
        for (SortOption option : SortOption.values()) {
            if (option.name().equalsIgnoreCase(name)) {
                return option;
            }
        }
        throw new SourceException("Unknown sort option '%s'", name);
    }

    /**
     * Performs an operation on the specified value and a list of entities.
     *
     * @param value    the value to be operated on
     * @param entities the list of entities to be processed
     */
    @Override
    public void operate(SortOption value, List<Entity> entities) {
        value.sort(entities);
    }


}
