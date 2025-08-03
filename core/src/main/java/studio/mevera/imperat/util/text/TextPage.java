package studio.mevera.imperat.util.text;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public record TextPage<T>(int index, int capacity, List<T> items) implements Iterable<T> {

    public void add(T obj) {
        if (items.size() + 1 > capacity) return;
        items.add(obj);
    }

    public void remove(T obj) {
        items.remove(obj);
    }

    public void addAll(List<T> otherItems) {
        otherItems.forEach(this::add);
    }


    @Override
    public @NotNull Iterator<T> iterator() {
        return items.iterator();
    }

    public List<T> asList() {
        return items;
    }
}
