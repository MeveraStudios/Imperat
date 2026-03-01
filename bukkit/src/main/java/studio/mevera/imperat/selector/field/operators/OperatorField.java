package studio.mevera.imperat.selector.field.operators;


import org.bukkit.entity.Entity;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.selector.field.AbstractField;
import studio.mevera.imperat.util.TypeWrap;

import java.util.List;

/**
 * OperatorField is an abstract base class that extends AbstractField.
 * It is designed to add additional operational functionality to the field it wraps.
 *
 * @param <V> The type of the value that this field handles.
 */
public abstract class OperatorField<V> extends AbstractField<V> implements OperatorFields {

    protected OperatorField(String name, TypeWrap<V> type) {
        super(name, type);
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
    public abstract V parseFieldValue(String value, CommandContext<BukkitSource> context) throws CommandException;

    /**
     * Performs an operation on the specified value and a list of entities.
     *
     * @param value    the value to be operated on
     * @param entities the list of entities to be processed
     */
    public abstract void operate(V value, List<Entity> entities);
}
