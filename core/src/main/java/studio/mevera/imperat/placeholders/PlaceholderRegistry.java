package studio.mevera.imperat.placeholders;

import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Registry;

public final class PlaceholderRegistry<S extends Source> extends Registry<String, Placeholder<S>> {

    private final ImperatConfig<S> imperat;

    PlaceholderRegistry(ImperatConfig<S> imperat) {
        this.imperat = imperat;
    }

    public static <S extends Source> PlaceholderRegistry<S> createDefault(ImperatConfig<S> imperat) {
        return new PlaceholderRegistry<>(imperat);
    }

    public String resolvedString(String input) {

        String result = input;
        for (var placeHolder : getAll()) {

            if (placeHolder.isUsedIn(result)) {
                String id = placeHolder.id();
                result = placeHolder.replaceResolved(imperat, id, result);
            }

        }
        return result;
    }

    public String[] resolvedArray(String[] array) {
        String[] arr = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            arr[i] = resolvedString(array[i]);
        }
        return arr;
    }


}
