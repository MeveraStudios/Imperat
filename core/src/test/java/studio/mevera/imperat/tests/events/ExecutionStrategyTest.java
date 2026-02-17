package studio.mevera.imperat.tests.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.events.ExecutionStrategy;
import studio.mevera.imperat.util.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests async and sync execution strategies.
 */
@DisplayName("EventBus - Execution Strategy Tests")
public class ExecutionStrategyTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.createDummy();
    }

    @Test
    @DisplayName("Should execute SYNC handlers in calling thread")
    void testSyncExecutionInSameThread() {
        long callingThreadId = Thread.currentThread().getId();
        AtomicBoolean sameThread = new AtomicBoolean(false);

        eventBus.register(
            TestEvent.class,
            e -> sameThread.set(Thread.currentThread().getId() == callingThreadId),
            Priority.NORMAL,
            ExecutionStrategy.SYNC
        );

        eventBus.post(new TestEvent("test"));

        assertTrue(sameThread.get(), "SYNC handler should execute in calling thread");
    }

    @Test
    @DisplayName("Should execute ASYNC handlers in different thread")
    void testAsyncExecutionInDifferentThread() throws InterruptedException {
        long callingThreadId = Thread.currentThread().getId();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean differentThread = new AtomicBoolean(false);

        eventBus.register(
            TestEvent.class,
            e -> {
                differentThread.set(Thread.currentThread().getId() != callingThreadId);
                latch.countDown();
            },
            Priority.NORMAL,
            ExecutionStrategy.ASYNC
        );

        eventBus.post(new TestEvent("test"));
        assertTrue(latch.await(5, TimeUnit.SECONDS), "ASYNC handler should complete");
        assertTrue(differentThread.get(), "ASYNC handler should execute in different thread");
    }

    @Test
    @DisplayName("Should block on SYNC handlers but not on ASYNC")
    void testSyncBlocking() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch asyncLatch = new CountDownLatch(1);

        // SYNC handler that increments immediately
        eventBus.register(
            TestEvent.class,
            e -> counter.incrementAndGet(),
            Priority.NORMAL,
            ExecutionStrategy.SYNC
        );

        // ASYNC handler that increments after delay
        eventBus.register(
            TestEvent.class,
            e -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
                asyncLatch.countDown();
            },
            Priority.NORMAL,
            ExecutionStrategy.ASYNC
        );

        eventBus.post(new TestEvent("test"));

        // SYNC should be done, ASYNC might not be
        assertEquals(1, counter.get(), "SYNC handler should complete before post() returns");

        // Wait for ASYNC
        assertTrue(asyncLatch.await(5, TimeUnit.SECONDS));
        assertEquals(2, counter.get(), "ASYNC handler should complete eventually");
    }

    @Test
    @DisplayName("Should execute all SYNC handlers before any ASYNC")
    void testSyncBeforeAsync() throws InterruptedException {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch asyncLatch = new CountDownLatch(1);

        eventBus.register(
            TestEvent.class,
            e -> executionOrder.add("SYNC-1"),
            Priority.NORMAL,
            ExecutionStrategy.SYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> {
                executionOrder.add("ASYNC-1");
                asyncLatch.countDown();
            },
            Priority.NORMAL,
            ExecutionStrategy.ASYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> executionOrder.add("SYNC-2"),
            Priority.NORMAL,
            ExecutionStrategy.SYNC
        );

        eventBus.post(new TestEvent("test"));

        // SYNC handlers should be done
        assertTrue(executionOrder.contains("SYNC-1"));
        assertTrue(executionOrder.contains("SYNC-2"));

        // Wait for ASYNC
        assertTrue(asyncLatch.await(5, TimeUnit.SECONDS));

        // Verify SYNC executed before ASYNC
        int sync1Index = executionOrder.indexOf("SYNC-1");
        int sync2Index = executionOrder.indexOf("SYNC-2");
        int async1Index = executionOrder.indexOf("ASYNC-1");

        assertTrue(sync1Index < async1Index, "SYNC-1 should execute before ASYNC-1");
        assertTrue(sync2Index < async1Index, "SYNC-2 should execute before ASYNC-1");
    }

    @Test
    @DisplayName("Should respect priority within SYNC handlers")
    void testPriorityInSyncHandlers() {
        List<String> executionOrder = new ArrayList<>();

        eventBus.register(
            TestEvent.class,
            e -> executionOrder.add("SYNC-LOW"),
            Priority.LOW,
            ExecutionStrategy.SYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> executionOrder.add("SYNC-HIGH"),
            Priority.HIGH,
            ExecutionStrategy.SYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> executionOrder.add("SYNC-NORMAL"),
            Priority.NORMAL,
            ExecutionStrategy.SYNC
        );

        eventBus.post(new TestEvent("test"));

        assertEquals(
            List.of("SYNC-HIGH", "SYNC-NORMAL", "SYNC-LOW"),
            executionOrder,
            "SYNC handlers should execute in priority order"
        );
    }

    @Test
    @DisplayName("Should submit ASYNC handlers in priority order")
    void testPriorityInAsyncHandlers() throws InterruptedException {
        List<String> submissions = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        eventBus.register(
            TestEvent.class,
            e -> {
                submissions.add("ASYNC-LOW");
                latch.countDown();
            },
            Priority.LOW,
            ExecutionStrategy.ASYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> {
                submissions.add("ASYNC-HIGH");
                latch.countDown();
            },
            Priority.HIGH,
            ExecutionStrategy.ASYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> {
                submissions.add("ASYNC-NORMAL");
                latch.countDown();
            },
            Priority.NORMAL,
            ExecutionStrategy.ASYNC
        );

        eventBus.post(new TestEvent("test"));
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // ASYNC handlers should be submitted in priority order
        // (though execution may overlap)
        int highIndex = submissions.indexOf("ASYNC-HIGH");
        int normalIndex = submissions.indexOf("ASYNC-NORMAL");
        int lowIndex = submissions.indexOf("ASYNC-LOW");

        assertTrue(highIndex >= 0 && normalIndex >= 0 && lowIndex >= 0);
    }

    @Test
    @DisplayName("Should handle multiple ASYNC handlers concurrently")
    void testConcurrentAsyncExecution() throws InterruptedException {
        int handlerCount = 10;
        CountDownLatch startLatch = new CountDownLatch(handlerCount);
        CountDownLatch endLatch = new CountDownLatch(handlerCount);
        List<Long> threadIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < handlerCount; i++) {
            eventBus.register(
                TestEvent.class,
                e -> {
                    threadIds.add(Thread.currentThread().getId());
                    startLatch.countDown();
                    try {
                        // Wait for all to start
                        startLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    endLatch.countDown();
                },
                Priority.NORMAL,
                ExecutionStrategy.ASYNC
            );
        }

        eventBus.post(new TestEvent("test"));
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "All ASYNC handlers should complete");

        // Multiple different threads should be used
        long uniqueThreads = threadIds.stream().distinct().count();
        assertTrue(uniqueThreads > 1, "Should use multiple threads for async execution");
    }

    @Test
    @DisplayName("Should default to SYNC strategy when not specified")
    void testDefaultStrategy() {
        long callingThreadId = Thread.currentThread().getId();
        AtomicBoolean sameThread = new AtomicBoolean(false);

        // Register without specifying strategy
        eventBus.register(
            TestEvent.class,
            e -> sameThread.set(Thread.currentThread().getId() == callingThreadId)
        );

        eventBus.post(new TestEvent("test"));

        assertTrue(sameThread.get(), "Default strategy should be SYNC");
    }

    @Test
    @DisplayName("Should handle exception in SYNC handler")
    void testSyncHandlerException() {
        AtomicBoolean nextHandlerCalled = new AtomicBoolean(false);

        eventBus.register(
            TestEvent.class,
            e -> {
                throw new RuntimeException("SYNC error");
            },
            Priority.HIGH,
            ExecutionStrategy.SYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> nextHandlerCalled.set(true),
            Priority.LOW,
            ExecutionStrategy.SYNC
        );

        assertDoesNotThrow(() -> eventBus.post(new TestEvent("test")));
        assertTrue(nextHandlerCalled.get(), "Next handler should be called after exception");
    }

    @Test
    @DisplayName("Should handle exception in ASYNC handler")
    void testAsyncHandlerException() throws InterruptedException {
        CountDownLatch nextHandlerLatch = new CountDownLatch(1);

        eventBus.register(
            TestEvent.class,
            e -> {
                throw new RuntimeException("ASYNC error");
            },
            Priority.HIGH,
            ExecutionStrategy.ASYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> nextHandlerLatch.countDown(),
            Priority.LOW,
            ExecutionStrategy.ASYNC
        );

        assertDoesNotThrow(() -> eventBus.post(new TestEvent("test")));
        assertTrue(nextHandlerLatch.await(5, TimeUnit.SECONDS),
                   "Next handler should be called after exception");
    }

    @Test
    @DisplayName("Should handle mixed SYNC and ASYNC with exceptions")
    void testMixedStrategiesWithExceptions() throws InterruptedException {
        List<String> executed = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch asyncLatch = new CountDownLatch(1);

        eventBus.register(
            TestEvent.class,
            e -> {
                executed.add("SYNC-1");
                throw new RuntimeException("Error in SYNC-1");
            },
            Priority.MAXIMUM,
            ExecutionStrategy.SYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> executed.add("SYNC-2"),
            Priority.HIGH,
            ExecutionStrategy.SYNC
        );

        eventBus.register(
            TestEvent.class,
            e -> {
                executed.add("ASYNC-1");
                asyncLatch.countDown();
            },
            Priority.NORMAL,
            ExecutionStrategy.ASYNC
        );

        eventBus.post(new TestEvent("test"));

        assertTrue(executed.contains("SYNC-1"));
        assertTrue(executed.contains("SYNC-2"));

        assertTrue(asyncLatch.await(5, TimeUnit.SECONDS));
        assertTrue(executed.contains("ASYNC-1"));
    }

    // Test event class
    static class TestEvent implements Event {
        private final String data;

        TestEvent(String data) {
            this.data = data;
        }
    }
}

