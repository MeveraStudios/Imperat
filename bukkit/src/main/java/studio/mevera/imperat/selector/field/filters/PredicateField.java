package studio.mevera.imperat.selector.field.filters;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.selector.field.AbstractField;
import studio.mevera.imperat.util.TypeWrap;

/**
 * The PredicateField interface extends the SelectionField interface to add filtering
 * functionality based on a specific value. Implementing this interface allows
 * defining a condition that can be used to filter entities.
 *
 * @param <V> The type of the value that the filter field handles.
 */
public abstract class PredicateField<V> extends AbstractField<V> implements PredicateFields {

    /**
     * Constructs an AbstractField instance with the specified name and type.
     *
     * @param name The name of the selection field.
     * @param type The type information of the value that this field handles, wrapped in a TypeWrap object.
     */
    protected PredicateField(String name, TypeWrap<V> type) {
        super(name, type);
    }

    /**
     * Generates an {@link EntityCondition} based on the given value and command input stream.
     * This method is intended to be implemented by subclasses to provide specific
     * filtering conditions for entities.
     *
     * @param value              The value used to generate the condition.
     * @param commandInputStream The stream providing command input data.
     * @param context the context.
     * @return The condition that will be used to filter entities based on the value and input data.
     */
    @NotNull
    protected abstract EntityCondition getCondition(V value, CommandInputStream<BukkitSource> commandInputStream, Context<BukkitSource> context);

    public final boolean isApplicable(BukkitSource sender, Entity entity, V value, CommandInputStream<BukkitSource> commandInputStream, Context<BukkitSource> ctx) throws ImperatException {
        EntityCondition condition = getCondition(value, commandInputStream, ctx);
        return condition.test(sender, entity);
    }
}
