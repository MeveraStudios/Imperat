package studio.mevera.imperat.selector;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public final class TargetSelector implements Iterable<Entity> {

    private final SelectionType type;
    private final List<Entity> selectedEntities;

    private TargetSelector(SelectionType type, List<Entity> selectedEntities) {
        this.type = type;
        this.selectedEntities = selectedEntities;
    }

    public static TargetSelector of(SelectionType type, List<Entity> list) {
        return new TargetSelector(type, list);
    }

    public static TargetSelector of(SelectionType type, Entity... list) {
        return of(type, List.of(list));
    }

    public static TargetSelector empty() {
        return of(SelectionType.UNKNOWN, List.of());
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public @NotNull Iterator<Entity> iterator() {
        return selectedEntities.iterator();
    }

    public SelectionType getType() {
        return type;
    }

    public int size() {
        return selectedEntities.size();
    }

    public boolean isEmpty() {
        return selectedEntities.isEmpty();
    }

    public boolean containsExactly(final Entity... entities) {
        if (entities.length != selectedEntities.size()) {
            return false;
        }
        for (final Entity entity : entities) {
            if (!selectedEntities.contains(entity)) {
                return false;
            }
        }
        return true;
    }

    public <E extends Entity> List<E> only(final Class<E> type) {
        return selectedEntities.stream()
                       .filter(type::isInstance)
                       .map(type::cast)
                       .toList();
    }

}
