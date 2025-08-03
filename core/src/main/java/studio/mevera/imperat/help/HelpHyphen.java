package studio.mevera.imperat.help;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

/**
 * Represents dynamic function returning a hyphen string
 * used in headers and footers for {@link HelpHyphen}
 *
 * @param <S>
 */
@ApiStatus.AvailableSince("1.0.0")
@FunctionalInterface
public interface HelpHyphen<S extends Source> {

    /**
     * @param content the content of this hyphen
     * @return the value of the {@link HyphenContent}
     */
    @NotNull
    String value(HyphenContent<S> content);
}
