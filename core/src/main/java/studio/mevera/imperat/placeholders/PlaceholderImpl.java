package studio.mevera.imperat.placeholders;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.Source;

import java.util.Objects;
import java.util.regex.Pattern;

final class PlaceholderImpl<S extends Source> implements Placeholder<S> {
    private final String id;
    private final PlaceholderResolver<S> resolver;

    private final Pattern pattern;

    PlaceholderImpl(String id, PlaceholderResolver<S> resolver) {
        this.id = id;
        this.resolver = resolver;
        this.pattern = Pattern.compile(id);
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull PlaceholderResolver<S> resolver() {
        return resolver;
    }

    @Override
    public boolean isUsedIn(String input) {
        return pattern.matcher(input).find();
    }

    @Override
    public String replaceResolved(ImperatConfig<S> imperat, String id, String input) {
        assert isUsedIn(input);
        return pattern.matcher(input).replaceAll(
            resolveInput(id, imperat)
        );
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlaceholderImpl<?>) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
