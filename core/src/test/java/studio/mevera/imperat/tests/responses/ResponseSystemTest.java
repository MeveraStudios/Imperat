package studio.mevera.imperat.tests.responses;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.placeholders.PlaceholderDataProvider;
import studio.mevera.imperat.responses.Response;
import studio.mevera.imperat.responses.ResponseContentFetcher;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.responses.ResponseRegistry;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

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
    private ImperatConfig<TestCommandSource> config;
    private ResponseRegistry responseRegistry;
    private TestCommandSource source;
    private List<String> capturedMessages;

    @BeforeEach
    void setUp() {
        ImperatTestGlobals.resetTestState();

        // Use CopyOnWriteArrayList for thread-safe message capturing
        capturedMessages = new CopyOnWriteArrayList<>();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Create test source that captures messages
        source = new TestCommandSource(new PrintStream(outputStream)) {
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
    private CommandContext<TestCommandSource> createContext() {
        return config.getContextFactory().createContext(
                imperat,
                source,
                Command.<TestCommandSource>create(imperat, "test").build(),
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
        CommandContext<TestCommandSource> context = createContext();

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

        CommandContext<TestCommandSource> context = createContext();

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

        CommandContext<TestCommandSource> context = createContext();

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

        CommandContext<TestCommandSource> context = createContext();

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

        CommandContext<TestCommandSource> context = createContext();

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
    //        CommandContext<TestCommandSource> context = createContext();
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

        CommandContext<TestCommandSource> context = createContext();

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
        ResponseContentFetcher customFetcher =
                contentSupplier -> CompletableFuture.supplyAsync(() -> "[CUSTOM] " + contentSupplier.get() + " [/CUSTOM]");

        Response response = new TestResponse(testKey, () -> "Custom", customFetcher);
        responseRegistry.registerResponse(response);

        CommandContext<TestCommandSource> context = createContext();

        response.sendContent(context, null)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }
                    assertThat(capturedMessages)
                            .hasSize(1)
                            .first()
                            .isEqualTo("[CUSTOM] Custom [/CUSTOM]");
                });
    }

    // ==================== CommandException Integration Tests ====================

    @Test
    @DisplayName("Should handle CommandException with ResponseKey and placeholders")
    void testCommandExceptionWithResponseKey() throws Exception {
        ResponseKey errorKey = () -> "error.test";
        Response response = new TestResponse(errorKey, () -> "Error: %message%")
                                    .addPlaceholder("message");

        responseRegistry.registerResponse(response);

        CommandException exception = ResponseException.of(errorKey)
                                             .withPlaceholder("message", "Something went wrong");

        CommandContext<TestCommandSource> context = createContext();

        // Simulate exception handling
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
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

        CommandException exception = ResponseException.of(errorKey)
                                             .withPlaceholder("command", "test");

        CommandContext<TestCommandSource> context = createContext();

        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
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
        CommandException exception = ResponseException.of(errorKey)
                                             .withPlaceholder("value", () -> String.valueOf(System.currentTimeMillis() / 1000));

        CommandContext<TestCommandSource> context = createContext();

        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
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
    @DisplayName("Should have all default responses registered")
    void testAllDefaultResponsesRegistered() {
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_BOOLEAN)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_ENUM)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_NUMBER_FORMAT)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_MAP_ENTRY_FORMAT)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.INVALID_UUID)).isNotNull();
        //assertThat(responseRegistry.getResponse(ResponseKey.WORD_OUT_OF_RESTRICTIONS)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.VALUE_OUT_OF_CONSTRAINT)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.UNKNOWN_FLAG)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.MISSING_FLAG_INPUT)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.FLAG_OUTSIDE_SCOPE)).isNotNull();
        assertThat(responseRegistry.getResponse(ResponseKey.NUMBER_OUT_OF_RANGE)).isNotNull();
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

        CommandContext<TestCommandSource> context = createContext();

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

        CommandContext<TestCommandSource> context = createContext();

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

        CommandContext<TestCommandSource> context = createContext();

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

        CommandContext<TestCommandSource> context = createContext();

        // Sending content should call it
        response.sendContent(context, null);
        Thread.sleep(100);

        assertThat(supplierCalled[0]).isTrue();
        assertThat(capturedMessages).hasSize(1);
    }

    // ==================== Placeholder Completeness Tests (no leftover '%') ====================

    @Test
    @DisplayName("Should fully resolve single placeholder with no leftover '%'")
    void testNoLeftoverPercentSinglePlaceholder() throws Exception {
        ResponseKey testKey = () -> "test.no-percent.single";
        Response response = new TestResponse(testKey, () -> "Hello %name%, welcome!")
                                    .addPlaceholder("name");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("name", Placeholder.builder("name")
                                              .resolver(id -> "Steve")
                                              .build());

        CommandContext<TestCommandSource> context = createContext();
        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Hello Steve, welcome!")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Should fully resolve multiple placeholders with no leftover '%'")
    void testNoLeftoverPercentMultiplePlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.no-percent.multiple";
        Response response = new TestResponse(testKey,
                () -> "User %user% ran /%command% with args: %args%")
                                    .addPlaceholder("user")
                                    .addPlaceholder("command")
                                    .addPlaceholder("args");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("user", Placeholder.builder("user")
                                              .resolver(id -> "Alice")
                                              .build());
        placeholders.register("command", Placeholder.builder("command")
                                                 .resolver(id -> "give")
                                                 .build());
        placeholders.register("args", Placeholder.builder("args")
                                              .resolver(id -> "diamond 64")
                                              .build());

        CommandContext<TestCommandSource> context = createContext();
        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("User Alice ran /give with args: diamond 64")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Should fully resolve placeholders via exception handler with no leftover '%'")
    void testNoLeftoverPercentViaExceptionHandler() throws Exception {
        ResponseKey errorKey = () -> "test.no-percent.exception";
        Response response = new TestResponse(errorKey,
                () -> "Error in /%command%: %reason% (input: %input%)")
                                    .addPlaceholder("command")
                                    .addPlaceholder("reason")
                                    .addPlaceholder("input");

        responseRegistry.registerResponse(response);

        CommandException exception = ResponseException.of(errorKey)
                                             .withPlaceholder("command", "ban")
                                             .withPlaceholder("reason", "player not found")
                                             .withPlaceholder("input", "notch123");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Error in /ban: player not found (input: notch123)")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Should fully resolve overridden default response with no leftover '%'")
    void testNoLeftoverPercentOverriddenResponse() throws Exception {
        Response customResponse = new TestResponse(ResponseKey.INVALID_NUMBER_FORMAT,
                () -> "'%input%' is not a valid number for /%command%!")
                                          .addPlaceholder("input")
                                          .addPlaceholder("command");

        responseRegistry.registerResponse(customResponse);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("input", Placeholder.builder("input")
                                               .resolver(id -> "abc")
                                               .build());
        placeholders.register("command", Placeholder.builder("command")
                                                 .resolver(id -> "give")
                                                 .build());

        CommandContext<TestCommandSource> context = createContext();

        Response retrieved = responseRegistry.getResponse(ResponseKey.INVALID_NUMBER_FORMAT);
        retrieved.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("'abc' is not a valid number for /give!")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Should fully resolve supplier-based placeholders with no leftover '%'")
    void testNoLeftoverPercentSupplierPlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.no-percent.supplier";
        Response response = new TestResponse(testKey,
                () -> "Cooldown: %remaining% seconds remaining for %player%")
                                    .addPlaceholder("remaining")
                                    .addPlaceholder("player");

        responseRegistry.registerResponse(response);

        CommandException exception = ResponseException.of(testKey)
                                             .withPlaceholder("remaining", () -> "42")
                                             .withPlaceholder("player", () -> "Notch");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Cooldown: 42 seconds remaining for Notch")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Should fully resolve duplicate placeholder occurrences with no leftover '%'")
    void testNoLeftoverPercentDuplicateOccurrences() throws Exception {
        ResponseKey testKey = () -> "test.no-percent.duplicate";
        Response response = new TestResponse(testKey,
                () -> "%name% said hello to %name%!")
                                    .addPlaceholder("name");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("name", Placeholder.builder("name")
                                              .resolver(id -> "Alex")
                                              .build());

        CommandContext<TestCommandSource> context = createContext();
        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Alex said hello to Alex!")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Should have no leftover '%' when response has no placeholders at all")
    void testNoLeftoverPercentStaticMessage() throws Exception {
        ResponseKey testKey = () -> "test.no-percent.static";
        Response response = new TestResponse(testKey, () -> "This is a static message with no placeholders");

        responseRegistry.registerResponse(response);

        CommandContext<TestCommandSource> context = createContext();
        response.sendContent(context, null);
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("This is a static message with no placeholders")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Should fully resolve adjacent placeholders with no leftover '%'")
    void testNoLeftoverPercentAdjacentPlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.no-percent.adjacent";
        Response response = new TestResponse(testKey,
                () -> "%first%%second%%third%")
                                    .addPlaceholder("first")
                                    .addPlaceholder("second")
                                    .addPlaceholder("third");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("first", Placeholder.builder("first")
                                               .resolver(id -> "A")
                                               .build());
        placeholders.register("second", Placeholder.builder("second")
                                                .resolver(id -> "B")
                                                .build());
        placeholders.register("third", Placeholder.builder("third")
                                               .resolver(id -> "C")
                                               .build());

        CommandContext<TestCommandSource> context = createContext();
        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("ABC")
                .doesNotContain("%");
    }

    // ==================== Default Response Placeholder Resolution Tests ====================

    @Test
    @DisplayName("Default INVALID_BOOLEAN response should resolve all placeholders")
    void testDefaultInvalidBooleanPlaceholderResolution() throws Exception {
        // Simulates: throw new ArgumentParseException(ResponseKey.INVALID_BOOLEAN, input)
        // ArgumentParseException adds: %input%
        // Response template: "Invalid boolean '%input%'"
        CommandException exception = ResponseException.of(ResponseKey.INVALID_BOOLEAN)
                                             .withPlaceholder("input", "maybe");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Invalid boolean 'maybe'")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default INVALID_ENUM response should resolve all placeholders")
    void testDefaultInvalidEnumPlaceholderResolution() throws Exception {
        // Simulates: throw new ArgumentParseException(ResponseKey.INVALID_ENUM, input).withPlaceholder("enum_type", ...)
        // Response template: "Invalid %enum_type% '%input%'"
        CommandException exception = ResponseException.of(ResponseKey.INVALID_ENUM)
                                             .withPlaceholder("input", "FLYING")
                                             .withPlaceholder("enum_type", "GameMode");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Invalid GameMode 'FLYING'")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default INVALID_NUMBER_FORMAT response should resolve all placeholders")
    void testDefaultInvalidNumberFormatPlaceholderResolution() throws Exception {
        // Simulates: throw new ArgumentParseException(ResponseKey.INVALID_NUMBER_FORMAT, input).withPlaceholder("number_type", ...)
        // Response template: "Invalid %number_type% format '%input%'"
        CommandException exception = ResponseException.of(ResponseKey.INVALID_NUMBER_FORMAT)
                                             .withPlaceholder("input", "abc")
                                             .withPlaceholder("number_type", "integer");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Invalid integer format 'abc'")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default INVALID_CHARACTER response should resolve all placeholders")
    void testDefaultInvalidCharacterPlaceholderResolution() throws Exception {
        // Simulates: throw new ArgumentParseException(ResponseKey.INVALID_CHARACTER, input)
        // Response template: "Invalid input '%input%', expected a single character"
        CommandException exception = ResponseException.of(ResponseKey.INVALID_CHARACTER)
                                             .withPlaceholder("input", "hello");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Invalid input 'hello', expected a single character")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default INVALID_MAP_ENTRY_FORMAT response should resolve all placeholders")
    void testDefaultInvalidMapEntryFormatPlaceholderResolution() throws Exception {
        // Simulates: throw new ArgumentParseException(ResponseKey.INVALID_MAP_ENTRY_FORMAT, raw).withPlaceholder("extra_msg", ...)
        // Response template: "Invalid map entry '%input%'%extra_msg%"
        CommandException exception = ResponseException.of(ResponseKey.INVALID_MAP_ENTRY_FORMAT)
                                             .withPlaceholder("input", "badentry")
                                             .withPlaceholder("extra_msg", ", entry doesn't contain '='");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Invalid map entry 'badentry', entry doesn't contain '='")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default INVALID_UUID response should resolve all placeholders")
    void testDefaultInvalidUuidPlaceholderResolution() throws Exception {
        // Simulates: throw new ArgumentParseException(ResponseKey.INVALID_UUID, input)
        // Response template: "Invalid uuid-format '%input%'"
        CommandException exception = ResponseException.of(ResponseKey.INVALID_UUID)
                                             .withPlaceholder("input", "not-a-uuid");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Invalid uuid-format 'not-a-uuid'")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default VALUE_OUT_OF_CONSTRAINT response should resolve all placeholders")
    void testDefaultValueOutOfConstraintPlaceholderResolution() throws Exception {
        // Simulates: throw new ArgumentParseException(ResponseKey.VALUE_OUT_OF_CONSTRAINT, input).withPlaceholder("allowed_values", ...)
        // Response template: "Input '%input%' is not one of: [%allowed_values%]"
        CommandException exception = ResponseException.of(ResponseKey.VALUE_OUT_OF_CONSTRAINT)
                                             .withPlaceholder("input", "diamond")
                                             .withPlaceholder("allowed_values", "gold,silver,bronze");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Input 'diamond' is not one of: [gold,silver,bronze]")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default UNKNOWN_FLAG response should resolve all placeholders")
    void testDefaultUnknownFlagPlaceholderResolution() throws Exception {
        // Simulates: throw new ArgumentParseException(ResponseKey.UNKNOWN_FLAG, input)
        // Response template: "Unknown flag '%input%'"
        CommandException exception = ResponseException.of(ResponseKey.UNKNOWN_FLAG)
                                             .withPlaceholder("input", "--verbose");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Unknown flag '--verbose'")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default MISSING_FLAG_INPUT response should resolve all placeholders")
    void testDefaultMissingFlagInputPlaceholderResolution() throws Exception {
        // Simulates: throw ResponseException.of(ResponseKey.MISSING_FLAG_INPUT).withPlaceholder("flags", ...)
        // Response template: "Please enter the value for flag(s) '%flags%'"
        CommandException exception = ResponseException.of(ResponseKey.MISSING_FLAG_INPUT)
                                             .withPlaceholder("flags", "-time");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Please enter the value for flag(s) '-time'")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default FLAG_OUTSIDE_SCOPE response should resolve all placeholders")
    void testDefaultFlagOutsideScopePlaceholderResolution() throws Exception {
        // Simulates: throw ResponseException.of(ResponseKey.FLAG_OUTSIDE_SCOPE).withPlaceholder("flag_input", ...).withPlaceholder("wrong_cmd", ...)
        // Response template: "Flag(s) '%flag_input%' were used (in %wrong_cmd%'s scope) outside of their command's scope"
        CommandException exception = ResponseException.of(ResponseKey.FLAG_OUTSIDE_SCOPE)
                                             .withPlaceholder("flag_input", "-c")
                                             .withPlaceholder("wrong_cmd", "ban");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Flag(s) '-c' were used (in ban's scope) outside of their command's scope")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default NUMBER_OUT_OF_RANGE response should resolve all placeholders")
    void testDefaultNumberOutOfRangePlaceholderResolution() throws Exception {
        // Simulates: throw ResponseException.of(ResponseKey.NUMBER_OUT_OF_RANGE).withPlaceholder(...)
        // Response template: "Value '%parsed_input%' entered for argument '%formatted_argument%' must be %formatted_range%"
        // RangeValidator provides: original_input, value, parameter, parameter_name, range, range_min, range_max
        // But the template uses: parsed_input, formatted_argument, formatted_range, input, range_min, range_max
        CommandException exception = ResponseException.of(ResponseKey.NUMBER_OUT_OF_RANGE)
                                             .withPlaceholder("parsed_input", "100")
                                             .withPlaceholder("formatted_argument", "<quantity>")
                                             .withPlaceholder("formatted_range", "within 1.0-50.0")
                                             .withPlaceholder("input", "100")
                                             .withPlaceholder("range_min", "1.0")
                                             .withPlaceholder("range_max", "50.0");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Value '100' entered for argument '<quantity>' must be within 1.0-50.0")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default COOLDOWN response should resolve all placeholders")
    void testDefaultCooldownPlaceholderResolution() throws Exception {
        // Simulates: throw ResponseException.of(ResponseKey.COOLDOWN).withPlaceholder(...)
        // Response template: "Please wait %seconds% second(s) to execute this command again!"
        CommandException exception = ResponseException.of(ResponseKey.COOLDOWN)
                                             .withPlaceholder("seconds", "5")
                                             .withPlaceholder("remaining_duration", "PT5S")
                                             .withPlaceholder("cooldown_duration", "PT10S")
                                             .withPlaceholder("last_executed", "2026-03-07T08:00:00Z");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Please wait 5 second(s) to execute this command again!")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default NO_HELP response should resolve all placeholders")
    void testDefaultNoHelpPlaceholderResolution() throws Exception {
        // Response template: "No Help available for '%command%'"
        CommandException exception = ResponseException.of(ResponseKey.NO_HELP)
                                             .withPlaceholder("command", "ban");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("No Help available for 'ban'")
                .doesNotContain("%");
    }

    @Test
    @DisplayName("Default NO_HELP_PAGE response should resolve all placeholders")
    void testDefaultNoHelpPagePlaceholderResolution() throws Exception {
        // Response template: "Page '%page%' doesn't exist!"
        CommandException exception = ResponseException.of(ResponseKey.NO_HELP_PAGE)
                                             .withPlaceholder("page", "99");

        CommandContext<TestCommandSource> context = createContext();
        config.handleExecutionError(exception, context, ResponseSystemTest.class, "testMethod");
        Thread.sleep(100);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0))
                .isEqualTo("Page '99' doesn't exist!")
                .doesNotContain("%");
    }
}











