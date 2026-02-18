package studio.mevera.imperat.exception;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderDataProvider;
import studio.mevera.imperat.responses.ResponseKey;

import java.util.function.Supplier;

@ApiStatus.AvailableSince("1.0.0")
public class CommandException extends Exception {

    private final ResponseKey responseKey;
    private @Nullable PlaceholderDataProvider placeholderDataProvider;
    public CommandException(String message, Throwable cause) {
        super(message, cause);
        this.responseKey = null;
    }

    public CommandException(String message) {
        super(message);
        this.responseKey = null;
    }

    public CommandException() {
        super();
        this.responseKey = null;
    }

    public CommandException(ResponseKey responseKey, @Nullable PlaceholderDataProvider placeholderDataProvider) {
        super();
        this.responseKey = responseKey;
        this.placeholderDataProvider = placeholderDataProvider;
    }

    public CommandException(ResponseKey responseKey) {
        super();
        this.responseKey = responseKey;
    }

    public @Nullable ResponseKey getResponseKey() {
        return responseKey;
    }

    public @Nullable PlaceholderDataProvider getPlaceholderDataProvider() {
        return placeholderDataProvider;
    }

    /**
     * Sets exception data for placeholder resolution.
     * Only use this when throwing with a ResponseKey.
     */
    public CommandException withPlaceholder(String key, String value) {
        return withPlaceholder(key, () -> value);
    }


    public CommandException withPlaceholder(String key, Supplier<String> valueSupplier) {
        return withPlaceholder(
                Placeholder.builder(key)
                        .resolver((id) -> valueSupplier.get())
                        .build()
        );
    }

    public <S extends Source> CommandException withContextPlaceholders(Context<S> ctx) {
        // e.g: %command%, %arguments%, etc.
        return withPlaceholder("command", ctx.command().name())
                       .withPlaceholder("arguments", String.join(" ", ctx.arguments()))
                       .withPlaceholder("source", ctx.source().name());
    }

    public CommandException withPlaceholder(Placeholder placeholder) {
        if (placeholderDataProvider == null) {
            placeholderDataProvider = PlaceholderDataProvider.createDefault();
        }
        placeholderDataProvider.register(placeholder.id(), placeholder);
        return this;
    }

}
