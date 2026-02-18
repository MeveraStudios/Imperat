package studio.mevera.imperat.placeholders;

final class DefaultPlaceholderDataProvider implements PlaceholderDataProvider {

    private final PlaceholderRegistry placeholderRegistry = PlaceholderRegistry.createDefault();

    @Override
    public PlaceholderRegistry registry() {
        return placeholderRegistry;
    }
}
