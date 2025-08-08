package studio.mevera.imperat.selector.field.filters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.util.TypeWrap;

final class NameField extends PredicateField<String> {
    /**
     * Constructs an AbstractField instance with the specified name and type.
     *
     * @param name The name of the selection field.
     */
    NameField(String name) {
        super(name, TypeWrap.of(String.class));
    }

    @Override
    protected @NotNull EntityCondition getCondition(String value, CommandInputStream<BukkitSource> commandInputStream, Context<BukkitSource> context) {
        return (sender, entity) -> entity.getName().equalsIgnoreCase(value);
    }

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value   the string representation of the value to be parsed
     * @param context
     * @return the parsed value of the field's type
     */
    @Override
    public String parseFieldValue(String value, Context<BukkitSource> context) {
        return value;
    }

}
