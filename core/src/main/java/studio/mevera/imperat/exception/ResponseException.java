package studio.mevera.imperat.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderDataProvider;
import studio.mevera.imperat.responses.ResponseKey;

import java.util.function.Supplier;

/**
 * Base class for all exceptions whose user-facing message is driven by the
 * {@link studio.mevera.imperat.responses.ResponseRegistry} — i.e. argument parsing
 * failures, flag errors, validation violations, and cooldowns.
 * <p>
 * The central throwable resolver handles every {@code ResponseException} uniformly:
 * it looks up the {@link ResponseKey} in the registry and sends the resolved content
 * (with placeholders) to the source.
 * <p>
 * <strong>NOT</strong> for structural/flow exceptions like {@link InvalidSyntaxException}
 * or {@link PermissionDeniedException} — those extend {@link CommandException} directly
 * and carry their own plain message.
 */
public abstract class ResponseException extends CommandException {

    private final @NotNull ResponseKey responseKey;
    private @Nullable PlaceholderDataProvider placeholderDataProvider;

    /**
     * @param responseKey the registry key used to look up the response message
     */
    protected ResponseException(@NotNull ResponseKey responseKey) {
        super();
        this.responseKey = responseKey;
    }

    protected ResponseException(@NotNull ResponseKey responseKey,
            @Nullable PlaceholderDataProvider placeholderDataProvider) {
        super();
        this.responseKey = responseKey;
        this.placeholderDataProvider = placeholderDataProvider;
    }

    /** Factory for creating an inline {@code ResponseException} with the given key. */
    public static ResponseException of(@NotNull ResponseKey responseKey) {
        return new ResponseException(responseKey) {
        };
    }

    /** Guaranteed non-null — every {@code ResponseException} must carry a key. */
    public @NotNull ResponseKey getResponseKey() {
        return responseKey;
    }

    public @Nullable PlaceholderDataProvider getPlaceholderDataProvider() {
        return placeholderDataProvider;
    }

    public ResponseException withPlaceholder(String key, String value) {
        return withPlaceholder(key, () -> value);
    }

    public ResponseException withPlaceholder(String key, Supplier<String> valueSupplier) {
        return withPlaceholder(
                Placeholder.builder(key)
                        .resolver((id) -> valueSupplier.get())
                        .build()
        );
    }

    public <S extends CommandSource> ResponseException withContextPlaceholders(CommandContext<S> ctx) {
        return withPlaceholder("command", ctx.command().getName())
                       .withPlaceholder("arguments", String.join(" ", ctx.arguments()))
                       .withPlaceholder("source", ctx.source().name());
    }

    public ResponseException withPlaceholder(Placeholder placeholder) {
        if (placeholderDataProvider == null) {
            placeholderDataProvider = PlaceholderDataProvider.createDefault();
        }
        placeholderDataProvider.register(placeholder.id(), placeholder);
        return this;
    }
}

