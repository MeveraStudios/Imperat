package studio.mevera.imperat.placeholders;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

final class PlaceholderImpl implements Placeholder {

    public final static String PLACEHOLDER_PREFIX = "%";
    public final static String PLACEHOLDER_SUFFIX = "%";

    private final Pattern pattern;
    private final String id, formattedId;
    private final PlaceholderResolver resolver;


    PlaceholderImpl(String id, PlaceholderResolver resolver) {
        this.id = id;
        this.formattedId = formatPlaceholder(id);
        this.resolver = resolver;
        this.pattern = Pattern.compile(formattedId);
    }

    static String formatPlaceholder(String key, String prefix, String suffix) {
        return prefix + key + suffix;
    }

    static String formatPlaceholder(String key) {
        return formatPlaceholder(key, PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX);
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull String formattedId() {
        return formattedId;
    }

    @Override
    public @NotNull PlaceholderResolver resolver() {
        return resolver;
    }

    @Override
    public boolean isUsedIn(String input) {
        return pattern.matcher(input).find();
    }

    @Override
    public String replaceResolved(String id, String input) {
        assert isUsedIn(input);
        return pattern.matcher(input).replaceAll(
                resolveInput(id)
        );
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (PlaceholderImpl) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
