package studio.mevera.imperat.placeholders;

import studio.mevera.imperat.util.Registry;

public final class PlaceholderRegistry extends Registry<String, Placeholder> {


    PlaceholderRegistry() {
    }

    public static PlaceholderRegistry createDefault() {
        return new PlaceholderRegistry();
    }

    public String applyPlaceholders(String input) {

        String result = input;
        for (var placeHolder : getAll()) {

            if (placeHolder.isUsedIn(result)) {
                String id = placeHolder.id();
                result = placeHolder.replaceResolved(id, result);
            }

        }
        return result;
    }

    public String[] resolvedArray(String[] array) {
        String[] arr = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            arr[i] = applyPlaceholders(array[i]);
        }
        return arr;
    }


}
