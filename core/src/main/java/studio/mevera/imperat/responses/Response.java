package studio.mevera.imperat.responses;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderDataProvider;
import studio.mevera.imperat.util.ImperatDebugger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Response {

    private final ResponseKey key;
    private final Supplier<String> contentSupplier;
    private final ResponseContentFetcher contentFetcher;
    private final Set<String> possiblePlaceholders = new HashSet<>();

    protected Response(
            ResponseKey key,
            Supplier<String> contentSupplier,
            @Nullable ResponseContentFetcher contentFetcher
    ) {
        this.key = key;
        this.contentSupplier = contentSupplier;
        this.contentFetcher = contentFetcher;
    }

    protected Response(
            ResponseKey key,
            Supplier<String> contentSupplier
    ) {
        this.key = key;
        this.contentSupplier = contentSupplier;
        this.contentFetcher = null;
    }

    public final @NotNull ResponseKey getKey() {
        return key;
    }

    public @Nullable ResponseContentFetcher getContentFetcher() {
        return contentFetcher;
    }

    public Response addPlaceholder(String id) {
        possiblePlaceholders.add(id);
        return this;
    }

    public Response addContextPlaceholders() {
        return addPlaceholder("command")
                       .addPlaceholder("arguments")
                       .addPlaceholder("source");
    }

    public <S extends Source> void sendContent(Context<S> ctx, @Nullable PlaceholderDataProvider placeholders) {
        var src = ctx.source();
        getContent(ctx, placeholders)
                .thenAccept((content) -> {
                    if (content == null) {
                        throw new IllegalStateException(
                                "Failed to fetch content for response '" + key.getKey() + "'. Check previous logs for more details.");
                    }
                    src.reply(content);
                });
    }

    private <S extends Source> CompletableFuture<String> getContent(Context<S> ctx, @Nullable PlaceholderDataProvider placeholders) {
        var cfg = ctx.imperatConfig();
        ResponseRegistry responseRegistry = cfg.getResponseRegistry();

        ResponseContentFetcher contentFetcher = this.contentFetcher != null
                                                        ? this.contentFetcher
                                                        : responseRegistry.loadDefaultContentFetcher();

        return contentFetcher.fetch(contentSupplier)
                       .thenApply(content -> applyPlaceholders(content, placeholders))
                       .exceptionally((ex) -> {
                           ImperatDebugger.error("Failed to fetch content for response '" + key.getKey() + "', caused by: " + ex.getMessage());
                           return null;
                       });
    }

    private String applyPlaceholders(
            String content,
            @Nullable PlaceholderDataProvider placeholders
    ) {
        if (placeholders == null) {
            return content;
        }

        //we need to check if the possible placeholders are present in the data provider's registry.
        Set<Placeholder> unknownPlaceholders = placeholders.registry().getAll().stream()
                                                       .filter((placeholder) -> !possiblePlaceholders.contains(placeholder.id()))
                                                       .collect(Collectors.toSet());

        if (!unknownPlaceholders.isEmpty()) {
            throw new IllegalStateException("The response '" + key.getKey() + "' was provided with unknown placeholders: " +
                                                    unknownPlaceholders.stream().map(Placeholder::id).collect(Collectors.joining(", ")));
        }

        return placeholders.registry().applyPlaceholders(content);
    }

}
