package studio.mevera.imperat.placeholders;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.util.Preconditions;

public interface Placeholder {

    static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * The unique name for this placeholder
     *
     * @return the name for this placeholder
     */
    @NotNull
    String id();

    @NotNull
    String formattedId();

    /**
     * The dynamic resolver for this placeholder
     *
     * @return the {@link PlaceholderResolver} resolver
     */
    @NotNull
    PlaceholderResolver resolver();

    boolean isUsedIn(String input);

    default String resolveInput(String id) {
        return resolver().resolve(id);
    }

    String replaceResolved(String id, String input);

    final class Builder {

        private final String id;
        private PlaceholderResolver resolver = null;

        Builder(String id) {
            this.id = id;
        }

        public Builder resolver(PlaceholderResolver resolver) {
            this.resolver = resolver;
            return this;
        }

        public Placeholder build() {
            Preconditions.notNull(resolver, "resolver is not set in the placeholder-builder");
            return new PlaceholderImpl(id, resolver);
        }
    }
}
