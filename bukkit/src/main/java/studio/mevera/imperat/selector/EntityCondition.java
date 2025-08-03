package studio.mevera.imperat.selector;

import org.bukkit.entity.Entity;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.exception.ImperatException;

/**
 * This interface represents a condition to be evaluated on an entity based on certain criteria.
 * It is a functional interface and can be used as a lambda expression or method reference.
 */
@FunctionalInterface
public interface EntityCondition {

    /**
     * Evaluates the given condition on an entity based on the source.
     *
     * @param sender the source initiating the test, typically a command sender
     * @param entity the entity to be evaluated against the condition
     * @return true if the entity meets the condition based on the source, false otherwise
     * @throws ImperatException if there is an error during the evaluation
     */
    boolean test(BukkitSource sender, Entity entity) throws ImperatException;

    default EntityCondition and(EntityCondition other) {
        return (sender, entity) -> EntityCondition.this.test(sender, entity) && other.test(sender, entity);
    }
}
