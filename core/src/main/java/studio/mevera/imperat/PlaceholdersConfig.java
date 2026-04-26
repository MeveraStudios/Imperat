package studio.mevera.imperat;

import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.placeholders.Placeholder;

public final class PlaceholdersConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    PlaceholdersConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public PlaceholdersConfig<S> register(Placeholder placeholder) {
        config.registerPlaceholder(placeholder);
        return this;
    }
}
