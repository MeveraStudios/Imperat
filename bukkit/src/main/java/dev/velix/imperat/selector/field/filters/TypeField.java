package dev.velix.imperat.selector.field.filters;

import dev.velix.imperat.BukkitSource;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.exception.SourceException;
import dev.velix.imperat.selector.EntityCondition;
import dev.velix.imperat.util.TypeWrap;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

final class TypeField extends PredicateField<EntityType> {
    /**
     * Constructs an AbstractField instance with the specified name and type.
     *
     * @param name The name of the selection field.
     */
    TypeField(String name) {
        super(name, TypeWrap.of(EntityType.class));
        Arrays.stream(EntityType.values())
            .map(EntityType::name)
            .map(String::toLowerCase)
            .forEach(suggestions::add);
    }

    @Override
    protected @NotNull EntityCondition getCondition(EntityType value, CommandInputStream<BukkitSource> commandInputStream) {
        return (sender, entity) -> entity.getType() == value;
    }

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value the string representation of the value to be parsed
     * @return the parsed value of the field's type
     * @throws ImperatException if the parsing fails
     */
    @Override
    public EntityType parseFieldValue(String value) throws ImperatException {
        try {
            return EntityType.valueOf(value.toUpperCase());
        } catch (EnumConstantNotPresentException ex) {
            throw new SourceException("Unknown entity-type '%s'", value);
        }
    }
}
