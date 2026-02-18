package studio.mevera.imperat.tests.responses;

import studio.mevera.imperat.responses.Response;
import studio.mevera.imperat.responses.ResponseContentFetcher;
import studio.mevera.imperat.responses.ResponseKey;

import java.util.function.Supplier;

/**
 * Test helper to create Response objects with placeholders for testing.
 * Since Response constructors are protected, we extend it for test purposes.
 */
class TestResponse extends Response {

    public TestResponse(ResponseKey key, Supplier<String> contentSupplier) {
        super(key, contentSupplier);
    }

    public TestResponse(ResponseKey key, Supplier<String> contentSupplier, ResponseContentFetcher contentFetcher) {
        super(key, contentSupplier, contentFetcher);
    }

    @Override
    public Response addPlaceholder(String id) {
        return super.addPlaceholder(id);
    }
}

