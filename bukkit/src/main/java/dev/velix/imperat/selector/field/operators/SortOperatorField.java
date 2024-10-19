package dev.velix.imperat.selector.field.operators;

import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.exception.SourceException;
import dev.velix.imperat.util.TypeWrap;
import org.bukkit.entity.Entity;

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
     * @param value the string representation of the value to be parsed
     * @return the parsed value of the field's type
     * @throws ImperatException if the parsing fails
     */
    @Override
    public SortOption parseFieldValue(String value) throws ImperatException {
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
