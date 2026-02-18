package studio.mevera.imperat.responses;

import java.util.function.Supplier;

public interface ResponseRegistry {

    static ResponseRegistry createDefault() {
        return new ResponseRegistryImpl();
    }

    default ResponseContentFetcher loadDefaultContentFetcher() {
        return ResponseContentFetcher.blocking();
    }

    void registerResponse(Response response);

    default void registerResponse(
            ResponseKey key,
            Supplier<String> contentSupplier,
            ResponseContentFetcher contentFetcher
    ) {
        registerResponse(
                new Response(key, contentSupplier, contentFetcher)
        );
    }

    default void registerResponse(
            ResponseKey key,
            Supplier<String> contentSupplier
    ) {
        registerResponse(
                new Response(key, contentSupplier, null)
        );
    }

    Response getResponse(ResponseKey key);

}
