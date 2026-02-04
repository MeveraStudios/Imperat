package studio.mevera.imperat.selector.field.operators;

import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public enum SortOption {

    NEAREST(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(e.getLocation()))),

    FURTHEST((e1, e2) ->
                     Double.compare(e2.getLocation().distanceSquared(e2.getLocation()), e1.getLocation().distanceSquared(e1.getLocation()))),

    RANDOM(Comparator.comparing(e -> Math.random())) /*not sure if it works*/,

    ARBITRARY(null);

    private final static Random RANDOM_GENERATOR = new Random();

    private final Comparator<Entity> comparator;

    SortOption(Comparator<Entity> comparator) {
        this.comparator = comparator;
    }

    void sort(List<Entity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        if (this == RANDOM) {
            Collections.shuffle(entities, RANDOM_GENERATOR);
            return;
        }
        if (comparator != null) {
            entities.sort(comparator);
        }
    }
}