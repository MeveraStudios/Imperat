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
            ResponseContentFetcher contentFetcher,
            String... possiblePlaceholders
    ) {
        Response response = new Response(key, contentSupplier, contentFetcher);
        response.addContextPlaceholders();
        for (String placeholder : possiblePlaceholders) {
            response.addPlaceholder(placeholder);
        }

        registerResponse(
                response
        );
    }

    default void registerResponse(
            ResponseKey key,
            Supplier<String> contentSupplier,
            String... possiblePlaceholders
    ) {
        registerResponse(
                key,
                contentSupplier,
                null,
                possiblePlaceholders
        );
    }

    Response getResponse(ResponseKey key);

}
