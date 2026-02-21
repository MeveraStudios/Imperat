package studio.mevera.imperat.placeholders;

import org.jetbrains.annotations.NotNull;

public interface PlaceholderResolver {

    /**
     * Resolves a placeholder
     *
     * @param placeHolderId the id for the placeholder
     * @return the placeholder to return
     */
    @NotNull
    String resolve(String placeHolderId);


}
