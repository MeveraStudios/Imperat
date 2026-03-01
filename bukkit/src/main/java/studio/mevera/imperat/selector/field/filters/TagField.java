package studio.mevera.imperat.selector.field.filters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.util.TypeWrap;

final class TagField extends PredicateField<String> {

    TagField(String name) {
        super(name, TypeWrap.of(String.class));
    }


    @Override
    protected @NotNull EntityCondition getCondition(String value, Cursor<BukkitSource> cursor,
            CommandContext<BukkitSource> context) {
        return ((sender, entity) -> entity.hasMetadata(value));
    }

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value   the string representation of the value to be parsed
     * @param context
     * @return the parsed value of the field's type
     */
    @Override
    public String parseFieldValue(String value, CommandContext<BukkitSource> context) {
        return value;
    }
}
