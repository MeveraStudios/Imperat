package studio.mevera.imperat.placeholders;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Source;

public interface PlaceholderResolver<S extends Source> {

    /**
     * Resolves a placeholder
     *
     * @param placeHolderId the id for the placeholder
     * @param imperat       the imperat
     * @return the placeholder to return
     */
    @NotNull
    String resolve(String placeHolderId, ImperatConfig<S> imperat);


}
