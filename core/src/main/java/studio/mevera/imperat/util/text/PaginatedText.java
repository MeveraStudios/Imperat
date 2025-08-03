package studio.mevera.imperat.util.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PaginatedText<T> {

    private final List<T> objects = new ArrayList<>();
    private final int itemsPerPage;

    @NotNull
    private final Map<Integer, TextPage<T>> pages = new HashMap<>();

    public PaginatedText(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public int getMaxPages() {
        return pages.size();
    }

    public @Nullable TextPage<T> getPage(int index) {
        return pages.get(index);
    }

    public void add(T convertible) {
        objects.add(convertible);
    }

    public void paginate() {
        for (int i = 1; i <= objects.size(); i++) {
            T obj = objects.get(i - 1);
            //calculate the page from its index and the items per page
            int page = (int) Math.ceil((double) (i) / (itemsPerPage));

            pages.compute(page, (index, existingPage) -> {
                if (existingPage == null) {
                    List<T> list = new ArrayList<>(itemsPerPage);
                    list.add(obj);
                    return new TextPage<>(page, itemsPerPage, list);
                }

                existingPage.add(obj);
                return existingPage;
            });

        }
    }

    public void clear() {
        objects.clear();
        pages.clear();
    }
}
