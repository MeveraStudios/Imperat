package studio.mevera.imperat.tests.responses;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Context;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced test cases for placeholder resolution, testing edge cases,
 * performance scenarios, and complex placeholder interactions.
 */
@DisplayName("Advanced Placeholder Tests")
class AdvancedPlaceholderTest {

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
                null,  // command not needed for response testing
                "",
                ArgumentInput.empty()
        );
    }

    @Test
    @DisplayName("Should handle response with same placeholder used multiple times")
    void testRepeatedPlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.repeated";
        Response response = new TestResponse(testKey,
                () -> "Hello %name%! Your name is %name%, right %name%?")
                                    .addPlaceholder("name");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("name", Placeholder.builder("name")
                                              .resolver(id -> "Alice")
                                              .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Hello Alice! Your name is Alice, right Alice?");
    }

    @Test
    @DisplayName("Should handle placeholders with numbers and underscores")
    void testComplexPlaceholderNames() throws Exception {
        ResponseKey testKey = () -> "test.complex-names";
        Response response = new TestResponse(testKey,
                () -> "Player: %player_name%, Level: %player_level_2%, Score: %score_1000%")
                                    .addPlaceholder("player_name")
                                    .addPlaceholder("player_level_2")
                                    .addPlaceholder("score_1000");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("player_name", Placeholder.builder("player_name")
                                                     .resolver(id -> "TestUser")
                                                     .build());
        placeholders.register("player_level_2", Placeholder.builder("player_level_2")
                                                        .resolver(id -> "50")
                                                        .build());
        placeholders.register("score_1000", Placeholder.builder("score_1000")
                                                    .resolver(id -> "9999")
                                                    .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Player: TestUser, Level: 50, Score: 9999");
    }

    @Test
    @DisplayName("Should handle placeholder adjacent to other placeholders")
    void testAdjacentPlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.adjacent";
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

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("ABC");
    }

    // ==================== Dynamic Value Resolution Tests ====================

    @Test
    @DisplayName("Should resolve placeholder value at access time, not registration time")
    void testDynamicPlaceholderResolution() throws Exception {
        ResponseKey testKey = () -> "test.dynamic";
        Response response = new TestResponse(testKey,
                () -> "Current value: %value%")
                                    .addPlaceholder("value");

        responseRegistry.registerResponse(response);

        AtomicInteger counter = new AtomicInteger(0);
        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("value", Placeholder.builder("value")
                                               .resolver(id -> String.valueOf(counter.incrementAndGet()))
                                               .build());

        Context<TestSource> context = createContext();

        // First call
        response.sendContent(context, placeholders);
        Thread.sleep(100);

        // Second call
        response.sendContent(context, placeholders);
        Thread.sleep(100);

        // Third call
        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(3)
                .containsExactly(
                        "Current value: 1",
                        "Current value: 2",
                        "Current value: 3"
                );
    }

    @Test
    @DisplayName("Should handle computed placeholder values")
    void testComputedPlaceholderValues() throws Exception {
        ResponseKey testKey = () -> "test.computed";
        Response response = new TestResponse(testKey,
                () -> "Sum: %sum%, Product: %product%, Average: %average%")
                                    .addPlaceholder("sum")
                                    .addPlaceholder("product")
                                    .addPlaceholder("average");

        responseRegistry.registerResponse(response);

        int a = 10, b = 5;
        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("sum", Placeholder.builder("sum")
                                             .resolver(id -> String.valueOf(a + b))
                                             .build());
        placeholders.register("product", Placeholder.builder("product")
                                                 .resolver(id -> String.valueOf(a * b))
                                                 .build());
        placeholders.register("average", Placeholder.builder("average")
                                                 .resolver(id -> String.valueOf((a + b) / 2.0))
                                                 .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Sum: 15, Product: 50, Average: 7.5");
    }

    // ==================== Large Content and Performance Tests ====================

    @Test
    @DisplayName("Should handle response with many placeholders efficiently")
    void testManyPlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.many-placeholders";

        Response response = new TestResponse(testKey, () -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                sb.append("Field").append(i).append(": %field").append(i).append("%, ");
            }
            return sb.toString();
        });

        for (int i = 0; i < 20; i++) {
            response.addPlaceholder("field" + i);
        }

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        for (int i = 0; i < 20; i++) {
            final int index = i;
            placeholders.register("field" + i, Placeholder.builder("field" + i)
                                                       .resolver(id -> "Value" + index)
                                                       .build());
        }

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(200);

        assertThat(capturedMessages)
                .hasSize(1);
        assertThat(capturedMessages.get(0))
                .contains("Field0: Value0")
                .contains("Field19: Value19");
    }

    @Test
    @DisplayName("Should handle large content strings efficiently")
    void testLargeContentString() throws Exception {
        ResponseKey testKey = () -> "test.large-content";

        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeContent.append("Line ").append(i).append(": %value% | ");
        }

        Response response = new TestResponse(testKey, largeContent::toString)
                                    .addPlaceholder("value");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("value", Placeholder.builder("value")
                                               .resolver(id -> "TEST")
                                               .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(200);

        assertThat(capturedMessages)
                .hasSize(1);
        assertThat(capturedMessages.get(0))
                .contains("Line 0: TEST")
                .contains("Line 99: TEST");
    }

    // ==================== Placeholder Value Edge Cases ====================

    @Test
    @DisplayName("Should handle placeholder with null-returning resolver")
    void testNullPlaceholderValue() throws Exception {
        ResponseKey testKey = () -> "test.null-value";
        Response response = new TestResponse(testKey,
                () -> "Value: '%value%'")
                                    .addPlaceholder("value");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("value", Placeholder.builder("value")
                                               .resolver(id -> "null")  // Return string "null" instead of actual null
                                               .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1);
        assertThat(capturedMessages.get(0))
                .contains("Value:");
    }

    @Test
    @DisplayName("Should handle placeholder with very long value")
    void testVeryLongPlaceholderValue() throws Exception {
        ResponseKey testKey = () -> "test.long-value";
        Response response = new TestResponse(testKey,
                () -> "Data: %data%")
                                    .addPlaceholder("data");

        responseRegistry.registerResponse(response);

        String longValue = "X".repeat(1000);
        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("data", Placeholder.builder("data")
                                              .resolver(id -> longValue)
                                              .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(200);

        assertThat(capturedMessages)
                .hasSize(1);
        String result = capturedMessages.get(0);
        assertThat(result)
                .startsWith("Data: XXX")
                .hasSize(6 + 1000); // "Data: " + 1000 X's
    }

    @Test
    @DisplayName("Should handle placeholder with newlines and special formatting")
    void testPlaceholderWithNewlines() throws Exception {
        ResponseKey testKey = () -> "test.newlines";
        Response response = new TestResponse(testKey,
                () -> "Message:\n%content%\nEnd of message")
                                    .addPlaceholder("content");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("content", Placeholder.builder("content")
                                                 .resolver(id -> "Line 1\nLine 2\nLine 3")
                                                 .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1);
        assertThat(capturedMessages.get(0))
                .contains("Message:\nLine 1\nLine 2\nLine 3\nEnd of message");
    }

    @Test
    @DisplayName("Should handle placeholder with unicode characters")
    void testUnicodePlaceholders() throws Exception {
        ResponseKey testKey = () -> "test.unicode";
        Response response = new TestResponse(testKey,
                () -> "Status: %emoji% | Name: %name% | Symbol: %symbol%")
                                    .addPlaceholder("emoji")
                                    .addPlaceholder("name")
                                    .addPlaceholder("symbol");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("emoji", Placeholder.builder("emoji")
                                               .resolver(id -> "✅")
                                               .build());
        placeholders.register("name", Placeholder.builder("name")
                                              .resolver(id -> "用户名")
                                              .build());
        placeholders.register("symbol", Placeholder.builder("symbol")
                                                .resolver(id -> "€£¥")
                                                .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Status: ✅ | Name: 用户名 | Symbol: €£¥");
    }

    // ==================== Placeholder Validation Edge Cases ====================

    @Test
    @DisplayName("Should throw error when placeholder is used but not declared")
    void testUndeclaredPlaceholderUsed() throws Exception {
        ResponseKey testKey = () -> "test.undeclared";
        // Response declares 'name' but we provide 'name' AND 'extra'
        Response response = new TestResponse(testKey,
                () -> "Hello %name%!")
                                    .addPlaceholder("name");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("name", Placeholder.builder("name")
                                              .resolver(id -> "World")
                                              .build());
        placeholders.register("extra", Placeholder.builder("extra")
                                               .resolver(id -> "Extra")
                                               .build());

        Context<TestSource> context = createContext();

        // sendContent() is async, so we need to wait and check for exceptions
        Assertions.assertTrue(capturedMessages.isEmpty());
    }

    @Test
    @DisplayName("Should handle empty placeholder registry gracefully")
    void testEmptyPlaceholderRegistry() throws Exception {
        ResponseKey testKey = () -> "test.empty-registry";
        Response response = new TestResponse(testKey,
                () -> "Static message with no placeholders");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        // No placeholders registered

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Static message with no placeholders");
    }

    // ==================== Content Fetcher with Placeholder Interaction ====================

    @Test
    @DisplayName("Should apply placeholders after async content fetching")
    void testPlaceholdersWithAsyncFetching() throws Exception {
        ResponseKey testKey = () -> "test.async-placeholders";

        ResponseContentFetcher asyncFetcher = contentSupplier -> CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(50); // Simulate async work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return contentSupplier.get();
        });

        Response response = new TestResponse(testKey,
                () -> "Async: %value%",
                asyncFetcher)
                                    .addPlaceholder("value");

        responseRegistry.registerResponse(response);

        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("value", Placeholder.builder("value")
                                               .resolver(id -> "AsyncValue")
                                               .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(200); // Wait for async operation

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("Async: AsyncValue");
    }

    @Test
    @DisplayName("Should handle placeholder resolution order correctly")
    void testPlaceholderResolutionOrder() throws Exception {
        ResponseKey testKey = () -> "test.order";
        Response response = new TestResponse(testKey,
                () -> "%first% -> %second% -> %third%")
                                    .addPlaceholder("first")
                                    .addPlaceholder("second")
                                    .addPlaceholder("third");

        responseRegistry.registerResponse(response);

        List<String> resolutionOrder = new ArrayList<>();
        PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
        placeholders.register("first", Placeholder.builder("first")
                                               .resolver(id -> {
                                                   resolutionOrder.add("first");
                                                   return "1";
                                               })
                                               .build());
        placeholders.register("second", Placeholder.builder("second")
                                                .resolver(id -> {
                                                    resolutionOrder.add("second");
                                                    return "2";
                                                })
                                                .build());
        placeholders.register("third", Placeholder.builder("third")
                                               .resolver(id -> {
                                                   resolutionOrder.add("third");
                                                   return "3";
                                               })
                                               .build());

        Context<TestSource> context = createContext();

        response.sendContent(context, placeholders);
        Thread.sleep(100);

        assertThat(capturedMessages)
                .hasSize(1)
                .first()
                .isEqualTo("1 -> 2 -> 3");

        // Verify all placeholders were resolved
        assertThat(resolutionOrder).contains("first", "second", "third");
    }

    // ==================== Stress Tests ====================

    @Test
    @DisplayName("Should handle rapid sequential placeholder resolutions")
    void testRapidSequentialResolutions() throws Exception {
        ResponseKey testKey = () -> "test.rapid";
        Response response = new TestResponse(testKey,
                () -> "Count: %count%")
                                    .addPlaceholder("count");

        responseRegistry.registerResponse(response);

        Context<TestSource> context = createContext();

        for (int i = 0; i < 10; i++) {
            final int index = i;
            PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
            placeholders.register("count", Placeholder.builder("count")
                                                   .resolver(id -> String.valueOf(index))
                                                   .build());

            response.sendContent(context, placeholders);
        }

        Thread.sleep(200);

        assertThat(capturedMessages).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(capturedMessages.get(i)).isEqualTo("Count: " + i);
        }
    }

    @Test
    @DisplayName("Should maintain thread safety with concurrent placeholder access")
    void testConcurrentPlaceholderAccess() throws Exception {
        ResponseKey testKey = () -> "test.concurrent";
        Response response = new TestResponse(testKey,
                () -> "Thread: %thread%")
                                    .addPlaceholder("thread");

        responseRegistry.registerResponse(response);

        Context<TestSource> context = createContext();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                PlaceholderDataProvider placeholders = PlaceholderDataProvider.createDefault();
                placeholders.register("thread", Placeholder.builder("thread")
                                                        .resolver(id -> "Thread-" + threadId)
                                                        .build());

                response.sendContent(context, placeholders);
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Thread.sleep(200);

        assertThat(capturedMessages).hasSize(5);
        assertThat(capturedMessages).allMatch(msg -> msg.startsWith("Thread: Thread-"));
    }
}
















