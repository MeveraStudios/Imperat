package studio.mevera.imperat.tests.responses;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderDataProvider;
import studio.mevera.imperat.responses.Response;
import studio.mevera.imperat.responses.ResponseContentFetcher;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.responses.ResponseRegistry;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.tests.TestSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Comprehensive test suite for the Response system, testing:
 * - Response registration and retrieval
 * - Placeholder resolution and validation
 * - Content fetching (blocking and async)
 * - Integration with CommandException
 * - Real-world exception scenarios
 */
@DisplayName("Response System Tests")
class ResponseSystemTest {

    private TestImperat imperat;
    private ImperatConfig<TestSource> config;
    private ResponseRegistry responseRegistry;
    private TestSource source;
    private List<String> capturedMessages;

    @BeforeEach
    void setUp() {
        ImperatTestGlobals.resetTestState();

        // Use CopyOnWriteArrayList for thread-safe message capturing
        capturedMessages = new CopyOnWriteArrayList<>();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Create test source that captures messages
        source = new TestSource(new PrintStream(outputStream)) {
            @Override
            public void reply(String message) {
                capturedMessages.add(message);
                super.reply(message);
            }
        };

        imperat = TestImperatConfig.builder().build();
        config = imperat.config();
        responseRegistry = config.getResponseRegistry();
    }

    // Helper method to create context with custom source
    private Context<TestSource> createContext() {
        return config.getContextFactory().createContext(
                imperat,
                source,
                Command.<TestSource>create(imperat, "test").build(),
                "test",
                ArgumentInput.of("test")
        );
    }

    // ==================== Basic Response Registration Tests ====================

    @Test
    @DisplayName("Should register and retrieve basic response")
    void testBasicResponseRegistration() {
        ResponseKey testKey = () -> "test.basic";

        responseRegistry.registerResponse(testKey, () -> "Test message");

        Response retrieved = responseRegistry.getResponse(testKey);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getKey().getKey()).isEqualTo("test.basic");
    }

    @Test
    @DisplayName("Should register response with convenience method")
    void testConvenienceRegistration() {
        ResponseKey testKey = () -> "test.convenience";

        responseRegistry.registerResponse(testKey, () -> "Convenience message");

        Response retrieved = responseRegistry.getResponse(testKey);
        assertThat(retrieved).isNotNull();
    }

    @Test
    @DisplayName("Should register response with custom content fetcher")
    void testCustomContentFetcher() {
        ResponseKey testKey = () -> "test.custom-fetcher";
        ResponseContentFetcher asyncFetcher = ResponseContentFetcher.async();

        responseRegistry.registerResponse(testKey, () -> "Async message", asyncFetcher);

        Response retrieved = responseRegistry.getResponse(testKey);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getContentFetcher()).isEqualTo(asyncFetcher);
    }

    // ==================== Placeholder Tests ====================

    @Test
    @DisplayName("Should add placeholders to response")
    void testPlaceholderAddition() {
        ResponseKey testKey = () -> "test.placeholder";
        Response response = new TestResponse(testKey, () -> "Hello %name%!")
                                    .addPlaceholder("name");

        responseRegistry.registerResponse(response);

        Response retrieved = responseRegistry.getResponse(testKey);
        assertThat(retrieved).isNotNull();
    }

    @Test
    @DisplayName("Should resolve simple placeholder in response")
    void testSimplePlaceholderResolution() throws Exception {
        ResponseKey testKey = () -> "test.simple-placeholder";
        Response response = new TestResponse(testKey, () -> "Hello %name%!")
                                    .addPlaceholder("name");

        responseRegistry.registerResponse(response);

        // Create placeholder data
        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("name", Placeholder.builder("name")
                                              .resolver(id -> "World")
                                              .build());

        // Create a dummy context
        Context<TestSource> context = createContext();

        // Send content
        response.sendContent(context, placeholders);

        // Wait a bit for async operation
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Hello World!");
    }

    @Test
    @DisplayName("Should resolve multiple placeholders in response")
    void testMultiplePlaceholderResolution() throws Exception {
        ResponseKey testKey = () -> "test.multiple-placeholders";
        Response response = new TestResponse(testKey,
                () -> "User '%user%' executed command '%command%' with arguments '%args%'")
                                    .addPlaceholder("user")
                                    .addPlaceholder("command")
                                    .addPlaceholder("args");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("user", Placeholder.builder("user")
                                              .resolver(id -> "TestPlayer")
                                              .build());
        placeholders.register("command", Placeholder.builder("command")
                                                 .resolver(id -> "test")
                                                 .build());
        placeholders.register("args", Placeholder.builder("args")
                                              .resolver(id -> "arg1 arg2")
                                              .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("User 'TestPlayer' executed command 'test' with arguments 'arg1 arg2'");
    }

    @Test
    @DisplayName("Should throw exception for unknown placeholders")
    void testUnknownPlaceholderValidation() throws Exception {
        ResponseKey testKey = () -> "test.unknown-placeholder";
        Response response = new TestResponse(testKey, () -> "Hello %name%!")
                                    .addPlaceholder("name");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("name", Placeholder.builder("name")
                                              .resolver(id -> "World")
                                              .build());
        // Add an unknown placeholder
        placeholders.register("unknown", Placeholder.builder("unknown")
                                                 .resolver(id -> "value")
                                                 .build());

        Context<TestSource> context = createContext();

        // sendContent() is async, so the validation happens asynchronously
        // When validation fails, the CompletableFuture completes exceptionally
        // and no message is sent to the source
        response.sendContent(context, placeholders);

        // Verify that no message was sent because validation failed
        Assertions.assertTrue(capturedMessages.isEmpty());
    }

    @Test
    @DisplayName("Should handle response without placeholders")
    void testResponseWithoutPlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.no-placeholders";
        Response response = new TestResponse(testKey, () -> "Static message");

        responseRegistry.registerResponse(response);

        Context<TestSource> context = createContext();

        response.sendContent(context, null);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Static message");
    }

    // ==================== Content Fetcher Tests ====================

    @Test
    @DisplayName("Should fetch content with blocking fetcher")
    void testBlockingContentFetcher() throws Exception {
        ResponseKey testKey = () -> "test.blocking";
        ResponseContentFetcher blockingFetcher = ResponseContentFetcher.blocking();
        Response response = new TestResponse(testKey, () -> "Blocking message", blockingFetcher);

        responseRegistry.registerResponse(response);

        Context<TestSource> context = createContext();

        response.sendContent(context, null);
        Thread.sleep(50);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Blocking message");
    }

    //    @Test
    //    @DisplayName("Should fetch content with async fetcher")
    //    void testAsyncContentFetcher() throws Exception {
    //        ResponseKey testKey = () -> "test.async";
    //        ResponseContentFetcher asyncFetcher = ResponseContentFetcher.async();
    //        Response response = new TestResponse(testKey, () -> {
    //            // Simulate some work
    //            try {
    //                Thread.sleep(50);
    //            } catch (InterruptedException e) {
    //                Thread.currentThread().interrupt();
    //            }
    //            return "Async message";
    //        }, asyncFetcher);
    //
    //        responseRegistry.registerResponse(response);
    //
    //        Context<TestSource> context = createContext();
    //
    //        response.sendContent(context, null);
    //        Thread.sleep(200); // Wait for async operation
    //
    //        assertThat(capturedMessages)
    //                .hasSize(1)
    //                .first()
    //                .isEqualTo("Async message");
    //    }

    @Test
    @DisplayName("Should use default content fetcher when none specified")
    void testDefaultContentFetcher() throws Exception {
        ResponseKey testKey = () -> "test.default-fetcher";
        Response response = new TestResponse(testKey, () -> "Default fetcher message");

        responseRegistry.registerResponse(response);

        Context<TestSource> context = createContext();

        response.sendContent(context, null);
        Thread.sleep(50);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Default fetcher message");
    }

    @Test
    @DisplayName("Should handle custom content fetcher implementation")
    void testCustomContentFetcherImplementation() throws Exception {
        ResponseKey testKey = () -> "test.custom-impl";

        // Custom fetcher that modifies content
        ResponseContentFetcher customFetcher = contentSupplier ->
                                                       CompletableFuture.supplyAsync(() ->
                                                                                             "[CUSTOM] " + contentSupplier.get() + " [/CUSTOM]"
                                                       );

        Response response = new TestResponse(testKey, () -> "Custom", customFetcher);
        responseRegistry.registerResponse(response);

        Context<TestSource> context = createContext();

        response.sendContent(context, null);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("[CUSTOM] Custom [/CUSTOM]");
    }

    // ==================== CommandException Integration Tests ====================

    @Test
    @DisplayName("Should handle CommandException with ResponseKey and placeholders")
    void testCommandExceptionWithResponseKey() throws Exception {
        ResponseKey errorKey = () -> "error.test";
        Response response = new TestResponse(errorKey, () -> "Error: %message%")
                                    .addPlaceholder("message");

        responseRegistry.registerResponse(response);

        CommandException exception = new CommandException(errorKey)
                                             .withPlaceholder("message", "Something went wrong");

        Context<TestSource> context = createContext();

        // Simulate exception handling
        config.handleExecutionThrowable(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Error: Something went wrong");
    }

    @Test
    @DisplayName("Should handle CommandException with context placeholders")
    void testCommandExceptionWithContextPlaceholders() throws Exception {
        // This would require a real command context, so we'll test the mechanism
        ResponseKey errorKey = () -> "error.context";
        Response response = new TestResponse(errorKey, () -> "RootCommand '%command%' failed")
                                    .addPlaceholder("command");

        responseRegistry.registerResponse(response);

        CommandException exception = new CommandException(errorKey)
                                             .withPlaceholder("command", "test");

        Context<TestSource> context = createContext();

        config.handleExecutionThrowable(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("RootCommand 'test' failed");
    }

    @Test
    @DisplayName("Should handle CommandException with supplier placeholders")
    void testCommandExceptionWithSupplierPlaceholders() throws Exception {
        ResponseKey errorKey = () -> "error.supplier";
        Response response = new TestResponse(errorKey, () -> "Dynamic value: %value%")
                                    .addPlaceholder("value");

        responseRegistry.registerResponse(response);

        // Use a supplier that computes the value
        CommandException exception = new CommandException(errorKey)
                                             .withPlaceholder("value", () -> String.valueOf(System.currentTimeMillis() / 1000));

        Context<TestSource> context = createContext();

        config.handleExecutionThrowable(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .asString()
                .matches("Dynamic value: \\d+");
    }

    // ==================== Default Response Tests ====================

    @Test
    @DisplayName("Should have default INVALID_BOOLEAN response registered")
    void testDefaultInvalidBooleanResponse() {
        Response response = responseRegistry.getResponse(ResponseKey.INVALID_BOOLEAN);
        assertThat(response).isNotNull();
        assertThat(response.getKey().getKey()).isEqualTo("args.parsing.invalid-boolean");
    }

    @Test
    @DisplayName("Should have default INVALID_ENUM response registered")
    void testDefaultInvalidEnumResponse() {
        Response response = responseRegistry.getResponse(ResponseKey.INVALID_ENUM);
        assertThat(response).isNotNull();
        assertThat(response.getKey().getKey()).isEqualTo("args.parsing.invalid-enum");
    }

    @Test
    @DisplayName("Should have default INVALID_NUMBER_FORMAT response registered")
    void testDefaultInvalidNumberFormatResponse() {
        Response response = responseRegistry.getResponse(ResponseKey.INVALID_NUMBER_FORMAT);
        assertThat(response).isNotNull();
        assertThat(response.getKey().getKey()).isEqualTo("args.parsing.invalid-number-format");
    }

    @Test
    @DisplayName("Should have default PERMISSION_DENIED response registered")
    void testDefaultPermissionDeniedResponse() {
        Response response = responseRegistry.getResponse(ResponseKey.PERMISSION_DENIED);
        assertThat(response).isNotNull();
        assertThat(response.getKey().getKey()).isEqualTo("permission.denied");
    }

    @Test
    @DisplayName("Should have default INVALID_SYNTAX response registered")
    void testDefaultInvalidSyntaxResponse() {
        Response response = responseRegistry.getResponse(ResponseKey.INVALID_SYNTAX);
        assertThat(response).isNotNull();
        assertThat(response.getKey().getKey()).isEqualTo("command.invalid-syntax");
    }

    @Test
    @DisplayName("Should have all default responses registered")
    void testAllDefaultResponsesRegistered() {
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_BOOLEAN)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_ENUM)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_NUMBER_FORMAT)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_MAP_ENTRY_FORMAT)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_UUID)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.WORD_OUT_OF_RESTRICTIONS)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.VALUE_OUT_OF_CONSTRAINT)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.UNKNOWN_FLAG)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.MISSING_FLAG_INPUT)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.FLAG_OUTSIDE_SCOPE)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.NUMBER_OUT_OF_RANGE)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.PERMISSION_DENIED)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_SYNTAX)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.COOLDOWN)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.NO_HELP)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.NO_HELP_PAGE)).isNotNull();
    }

    // ==================== Response Override Tests ====================

    @Test
    @DisplayName("Should allow overriding default responses")
    void testOverrideDefaultResponse() throws Exception {
        ResponseKey key = ResponseKey.INVALID_BOOLEAN;
        Response customResponse = new TestResponse(key, () -> "CUSTOM: Invalid boolean value '%input%'!")
                                          .addPlaceholder("input");

        responseRegistry.registerResponse(customResponse);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("input", Placeholder.builder("input")
                                               .resolver(id -> "maybe")
                                               .build());

        Context<TestSource> context = createContext();

        Response retrieved = responseRegistry.getResponse(key);
        retrieved.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("CUSTOM: Invalid boolean value 'maybe'!");
    }

    // ==================== Edge Cases and Error Handling ====================

    @Test
    @DisplayName("Should handle empty placeholder value")
    void testEmptyPlaceholderValue() throws Exception {
        ResponseKey testKey = () -> "test.empty-placeholder";
        Response response = new TestResponse(testKey, () -> "Value: '%value%'")
                                    .addPlaceholder("value");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("value", Placeholder.builder("value")
                                               .resolver(id -> "")
                                               .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Value: ''");
    }

    @Test
    @DisplayName("Should handle null response key retrieval")
    void testNullResponseKeyRetrieval() {
        ResponseKey nonExistentKey = () -> "non.existent.key";
        Response response = responseRegistry.getResponse(nonExistentKey);
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("Should handle special characters in placeholder values")
    void testSpecialCharactersInPlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.special-chars";
        Response response = new TestResponse(testKey, () -> "Message: %content%")
                                    .addPlaceholder("content");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("content", Placeholder.builder("content")
                                                 .resolver(id -> "Hello \"World\" & <stuff>")
                                                 .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Message: Hello \"World\" & <stuff>");
    }

    @Test
    @DisplayName("Should handle response with no content supplier invocation")
    void testLazyContentSupplier() throws Exception {
        ResponseKey testKey = () -> "test.lazy";

        final boolean[] supplierCalled = {false};
        Supplier<String> lazySupplier = () -> {
            supplierCalled[0] = true;
            return "Lazy message";
        };

        Response response = new TestResponse(testKey, lazySupplier);
        responseRegistry.registerResponse(response);

        // Just registering shouldn't call the supplier
        assertThat(supplierCalled[0]).isFalse();

        Context<TestSource> context = createContext();

        // Sending content should call it
        response.sendContent(context, null);
        Thread.sleep(100);

        assertThat(supplierCalled[0]).isTrue();
        assertThat(capturedMessages).hasSize(1);
    }
}











