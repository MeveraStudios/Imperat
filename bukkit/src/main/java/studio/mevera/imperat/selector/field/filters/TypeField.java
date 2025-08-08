package studio.mevera.imperat.selector.field.filters;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.util.TypeWrap;

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
    protected @NotNull EntityCondition getCondition(EntityType value, CommandInputStream<BukkitSource> commandInputStream, Context<BukkitSource> context) {
        return (sender, entity) -> entity.getType() == value;
    }

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value   the string representation of the value to be parsed
     * @param context
     * @return the parsed value of the field's type
     * @throws ImperatException if the parsing fails
     */
    @Override
    public EntityType parseFieldValue(String value, Context<BukkitSource> context) throws ImperatException {
        try {
            return EntityType.valueOf(value.toUpperCase());
        } catch (EnumConstantNotPresentException ex) {
            throw new SourceException(context, "Unknown entity-type '%s'", value);
        }
    }
}
