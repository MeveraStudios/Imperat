package studio.mevera.imperat;

import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.responses.Response;
import studio.mevera.imperat.responses.ResponseKey;

import java.util.function.Supplier;

public final class ResponsesConfig<S extends CommandSource> {

    private final ImperatConfig<S> config;

    ResponsesConfig(ImperatConfig<S> config) {
        this.config = config;
    }

    public ResponsesConfig<S> register(Response response) {
        config.getResponseRegistry().registerResponse(response);
        return this;
    }

    public ResponsesConfig<S> register(ResponseKey key, Supplier<String> contentSupplier, String... placeholders) {
        config.getResponseRegistry().registerResponse(key, contentSupplier, placeholders);
        return this;
    }
}
