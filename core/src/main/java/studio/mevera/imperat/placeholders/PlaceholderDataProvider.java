package studio.mevera.imperat.placeholders;


import java.util.Optional;

public interface PlaceholderDataProvider {

    static PlaceholderDataProvider createDefault() {
        return new DefaultPlaceholderDataProvider();
    }

    PlaceholderRegistry registry();

    default void register(String id, Placeholder placeholder) {
        registry().setData(id, placeholder);
    }

    default Optional<Placeholder> get(String id) {
        return registry().getData(id);
    }


}
