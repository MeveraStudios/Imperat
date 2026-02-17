package studio.mevera.imperat.tests.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.events.CancellableEvent;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.events.EventExceptionHandler;
import studio.mevera.imperat.events.EventSubscription;
import studio.mevera.imperat.events.ExecutionStrategy;
import studio.mevera.imperat.util.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration tests for complex event system scenarios.
 */
@DisplayName("EventBus - Integration Tests")
public class EventBusIntegrationTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.createDummy();
    }

    @Test
    @DisplayName("Should handle complex workflow with multiple event types")
    void testComplexWorkflow() {
        List<String> workflow = Collections.synchronizedList(new ArrayList<>());

        // Register handlers for different events
        eventBus.register(WorkflowStartEvent.class, e -> {
            workflow.add("workflow-started");
            e.setData("initialized");
        }, Priority.MAXIMUM);

        eventBus.register(WorkflowProcessEvent.class, e -> {
            workflow.add("processing: " + e.getData());
            e.setData(e.getData() + "-processed");
        }, Priority.NORMAL);

        eventBus.register(WorkflowCompleteEvent.class, e -> {
            workflow.add("workflow-completed: " + e.getData());
        }, Priority.NORMAL);

        // Execute workflow
        WorkflowStartEvent startEvent = new WorkflowStartEvent();
        eventBus.post(startEvent);

        WorkflowProcessEvent processEvent = new WorkflowProcessEvent(startEvent.getData());
        eventBus.post(processEvent);

        WorkflowCompleteEvent completeEvent = new WorkflowCompleteEvent(processEvent.getData());
        eventBus.post(completeEvent);

        assertEquals(3, workflow.size());
        assertEquals("workflow-started", workflow.get(0));
        assertTrue(workflow.get(1).contains("processing"));
        assertTrue(workflow.get(2).contains("completed"));
    }

    @Test
    @DisplayName("Should handle dynamic handler registration and unregistration")
    void testDynamicHandlerManagement() {
        AtomicInteger callCount = new AtomicInteger(0);
        List<EventSubscription<TestEvent>> subscriptions = new ArrayList<>();

        // Register 5 handlers
        for (int i = 0; i < 5; i++) {
            EventSubscription<TestEvent> sub = eventBus.register(
                TestEvent.class,
                e -> callCount.incrementAndGet()
            );
            subscriptions.add(sub);
        }

        // Post event - all handlers should be called
        eventBus.post(new TestEvent("test1"));
        assertEquals(5, callCount.get());

        // Unregister 3 handlers
        for (int i = 0; i < 3; i++) {
            eventBus.unregister(subscriptions.get(i).id());
        }

        // Post event - only 2 handlers should be called
        eventBus.post(new TestEvent("test2"));
        assertEquals(7, callCount.get()); // 5 + 2

        // Register 2 more handlers
        for (int i = 0; i < 2; i++) {
            eventBus.register(TestEvent.class, e -> callCount.incrementAndGet());
        }

        // Post event - 4 handlers should be called (2 remaining + 2 new)
        eventBus.post(new TestEvent("test3"));
        assertEquals(11, callCount.get()); // 7 + 4
    }

    @Test
    @DisplayName("Should handle concurrent event posting")
    void testConcurrentPosting() throws InterruptedException {
        AtomicInteger eventCount = new AtomicInteger(0);

        eventBus.register(TestEvent.class, e -> {
            eventCount.incrementAndGet();
        });

        int threadCount = 10;
        int eventsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        eventBus.post(new TestEvent("test-" + j));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount * eventsPerThread, eventCount.get());
    }

    @Test
    @DisplayName("Should handle complex priority and strategy combinations")
    void testComplexPriorityAndStrategy() throws InterruptedException {
        List<String> execution = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch asyncLatch = new CountDownLatch(2);

        // Mix of sync and async handlers with different priorities
        eventBus.register(TestEvent.class, e -> {
            execution.add("SYNC-HIGHEST");
        }, Priority.MAXIMUM, ExecutionStrategy.SYNC);

        eventBus.register(TestEvent.class, e -> {
            execution.add("ASYNC-HIGH");
            asyncLatch.countDown();
        }, Priority.HIGH, ExecutionStrategy.ASYNC);

        eventBus.register(TestEvent.class, e -> {
            execution.add("SYNC-NORMAL");
        }, Priority.NORMAL, ExecutionStrategy.SYNC);

        eventBus.register(TestEvent.class, e -> {
            execution.add("ASYNC-LOW");
            asyncLatch.countDown();
        }, Priority.LOW, ExecutionStrategy.ASYNC);

        eventBus.register(TestEvent.class, e -> {
            execution.add("SYNC-LOWEST");
        }, Priority.MINIMUM, ExecutionStrategy.SYNC);

        eventBus.post(new TestEvent("test"));

        // Wait for ASYNC handlers to complete
        assertTrue(asyncLatch.await(5, TimeUnit.SECONDS), "Async handlers should complete within timeout");


        // All handlers should have been executed
        assertEquals(5, execution.size(), "All 5 handlers should have executed");
        assertTrue(execution.contains("SYNC-HIGHEST"));
        assertTrue(execution.contains("SYNC-NORMAL"));
        assertTrue(execution.contains("SYNC-LOWEST"));
        assertTrue(execution.contains("ASYNC-HIGH"));
        assertTrue(execution.contains("ASYNC-LOW"));

        // Verify SYNC handlers executed before ASYNC handlers
        // Since all SYNC handlers must complete before ANY ASYNC handlers start,
        // we need to find the last SYNC and first ASYNC in the execution list
        int syncHighestIndex = execution.indexOf("SYNC-HIGHEST");
        int syncNormalIndex = execution.indexOf("SYNC-NORMAL");
        int syncLowestIndex = execution.indexOf("SYNC-LOWEST");
        int asyncHighIndex = execution.indexOf("ASYNC-HIGH");
        int asyncLowIndex = execution.indexOf("ASYNC-LOW");

        // All indices should be valid
        assertTrue(syncHighestIndex >= 0, "SYNC-HIGHEST should be in execution list");
        assertTrue(syncNormalIndex >= 0, "SYNC-NORMAL should be in execution list");
        assertTrue(syncLowestIndex >= 0, "SYNC-LOWEST should be in execution list");
        assertTrue(asyncHighIndex >= 0, "ASYNC-HIGH should be in execution list");
        assertTrue(asyncLowIndex >= 0, "ASYNC-LOW should be in execution list");

        // Find the last SYNC handler index
        int lastSyncIndex = Math.max(syncHighestIndex, Math.max(syncNormalIndex, syncLowestIndex));

        // Find the first ASYNC handler index
        int firstAsyncIndex = Math.min(asyncHighIndex, asyncLowIndex);

        // SYNC handlers must all execute before ASYNC handlers
        assertTrue(lastSyncIndex < firstAsyncIndex,
                String.format("All SYNC handlers should execute before ASYNC handlers. Last SYNC at %d, First ASYNC at %d. Execution order: %s",
                        lastSyncIndex, firstAsyncIndex, execution));
    }

    @Test
    @DisplayName("Should handle cascading events")
    void testCascadingEvents() {
        List<String> cascade = new ArrayList<>();

        eventBus.register(PrimaryEvent.class, primaryEvent -> {
            cascade.add("primary");
            // Trigger secondary event
            eventBus.post(new SecondaryEvent(primaryEvent.getData()));
        });

        eventBus.register(SecondaryEvent.class, secondaryEvent -> {
            cascade.add("secondary: " + secondaryEvent.getData());
            // Trigger tertiary event
            eventBus.post(new TertiaryEvent(secondaryEvent.getData()));
        });

        eventBus.register(TertiaryEvent.class, tertiaryEvent -> {
            cascade.add("tertiary: " + tertiaryEvent.getData());
        });

        eventBus.post(new PrimaryEvent("initial"));

        assertEquals(3, cascade.size());
        assertEquals("primary", cascade.get(0));
        assertTrue(cascade.get(1).startsWith("secondary"));
        assertTrue(cascade.get(2).startsWith("tertiary"));
    }

    @Test
    @DisplayName("Should handle event filtering pattern")
    void testEventFiltering() {
        List<String> processed = new ArrayList<>();
        AtomicInteger filtered = new AtomicInteger(0);

        eventBus.register(FilterableEvent.class, event -> {
            if (event.shouldProcess()) {
                processed.add(event.getData());
            } else {
                filtered.incrementAndGet();
            }
        });

        eventBus.post(new FilterableEvent("data1", true));
        eventBus.post(new FilterableEvent("data2", false));
        eventBus.post(new FilterableEvent("data3", true));
        eventBus.post(new FilterableEvent("data4", false));

        assertEquals(2, processed.size());
        assertEquals(2, filtered.get());
        assertEquals("data1", processed.get(0));
        assertEquals("data3", processed.get(1));
    }

    @Test
    @DisplayName("Should handle event aggregation pattern")
    void testEventAggregation() {
        AggregatorEvent aggregator = new AggregatorEvent();

        eventBus.register(AggregatorEvent.class, event -> {
            event.addValue(10);
        }, Priority.MAXIMUM);

        eventBus.register(AggregatorEvent.class, event -> {
            event.addValue(20);
        }, Priority.HIGH);

        eventBus.register(AggregatorEvent.class, event -> {
            event.addValue(30);
        }, Priority.NORMAL);

        eventBus.post(aggregator);

        assertEquals(60, aggregator.getTotal());
        assertEquals(List.of(10, 20, 30), aggregator.getValues());
    }

    @Test
    @DisplayName("Should handle transaction-like pattern with rollback")
    void testTransactionPattern() {
        TransactionEvent transaction = new TransactionEvent();
        List<String> actions = new ArrayList<>();

        // Validation phase
        eventBus.register(TransactionEvent.class, event -> {
            actions.add("validate");
            if (event.getData().isEmpty()) {
                event.setCancelled(true);
                actions.add("validation-failed");
            }
        }, Priority.MAXIMUM);

        // Execution phase (only if not cancelled)
        eventBus.register(TransactionEvent.class, event -> {
            if (!event.isCancelled()) {
                actions.add("execute");
                event.setData(event.getData() + "-executed");
            }
        }, Priority.NORMAL);

        // Commit/Rollback phase
        eventBus.register(TransactionEvent.class, event -> {
            if (event.isCancelled()) {
                actions.add("rollback");
            } else {
                actions.add("commit");
            }
        }, Priority.MINIMUM);

        // Test successful transaction
        transaction.setData("valid-data");
        eventBus.post(transaction);
        assertTrue(actions.contains("validate"));
        assertTrue(actions.contains("execute"));
        assertTrue(actions.contains("commit"));
        assertFalse(actions.contains("rollback"));

        // Test failed transaction
        actions.clear();
        TransactionEvent failedTransaction = new TransactionEvent();
        failedTransaction.setData("");
        eventBus.post(failedTransaction);
        assertTrue(actions.contains("validation-failed"));
        assertTrue(actions.contains("rollback"));
        assertFalse(actions.contains("execute"));
        assertFalse(actions.contains("commit"));
    }

    @Test
    @DisplayName("Should handle observer pattern")
    void testObserverPattern() {
        List<String> observer1Notifications = new ArrayList<>();
        List<String> observer2Notifications = new ArrayList<>();
        List<String> observer3Notifications = new ArrayList<>();

        eventBus.register(ObservableEvent.class, event -> {
            observer1Notifications.add("Observer1: " + event.getData());
        });

        eventBus.register(ObservableEvent.class, event -> {
            observer2Notifications.add("Observer2: " + event.getData());
        });

        eventBus.register(ObservableEvent.class, event -> {
            observer3Notifications.add("Observer3: " + event.getData());
        });

        // Notify all observers
        eventBus.post(new ObservableEvent("state-changed"));

        assertEquals(1, observer1Notifications.size());
        assertEquals(1, observer2Notifications.size());
        assertEquals(1, observer3Notifications.size());

        assertTrue(observer1Notifications.get(0).contains("state-changed"));
        assertTrue(observer2Notifications.get(0).contains("state-changed"));
        assertTrue(observer3Notifications.get(0).contains("state-changed"));
    }

    @Test
    @DisplayName("Should handle complex exception scenarios")
    void testComplexExceptionScenarios() throws InterruptedException {
        List<String> exceptionLog = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch asyncLatch = new CountDownLatch(1);
        CountDownLatch exceptionLatch = new CountDownLatch(3); // Wait for all 3 exceptions

        EventExceptionHandler handler = new EventExceptionHandler() {
            @Override public <E extends Event> void handle(E event, Throwable exception, EventSubscription<E> subscription) {
                exceptionLog.add(exception.getMessage());
                exceptionLatch.countDown();
            }
        };

        EventBus bus = EventBus.builder()
            .exceptionHandler(handler)
            .build();

        // SYNC handler that throws
        bus.register(TestEvent.class, e -> {
            throw new RuntimeException("SYNC-1 error");
        }, Priority.MAXIMUM, ExecutionStrategy.SYNC);

        // SYNC handler that succeeds
        bus.register(TestEvent.class, e -> {
            // Success
        }, Priority.HIGH, ExecutionStrategy.SYNC);

        // SYNC handler that throws
        bus.register(TestEvent.class, e -> {
            throw new IllegalStateException("SYNC-2 error");
        }, Priority.NORMAL, ExecutionStrategy.SYNC);

        // ASYNC handler that throws
        bus.register(TestEvent.class, e -> {
            try {
                throw new IllegalArgumentException("ASYNC error");
            } finally {
                asyncLatch.countDown();
            }
        }, Priority.LOW, ExecutionStrategy.ASYNC);

        bus.post(new TestEvent("test"));

        // Wait for async handler to execute
        assertTrue(asyncLatch.await(5, TimeUnit.SECONDS), "Async handler should execute");

        // Wait for all exception handlers to complete
        assertTrue(exceptionLatch.await(5, TimeUnit.SECONDS), "All exception handlers should complete");

        assertEquals(3, exceptionLog.size(), "Should have logged 3 exceptions");
        assertTrue(exceptionLog.contains("SYNC-1 error"));
        assertTrue(exceptionLog.contains("SYNC-2 error"));
        assertTrue(exceptionLog.contains("ASYNC error"));
    }

    // Test event classes
    static class TestEvent implements Event {
        private final String data;
        TestEvent(String data) { this.data = data; }
        String getData() { return data; }
    }

    static class WorkflowStartEvent implements Event {
        private String data;
        String getData() { return data; }
        void setData(String data) { this.data = data; }
    }

    static class WorkflowProcessEvent implements Event {
        private String data;
        WorkflowProcessEvent(String data) { this.data = data; }
        String getData() { return data; }
        void setData(String data) { this.data = data; }
    }

    static class WorkflowCompleteEvent implements Event {
        private final String data;
        WorkflowCompleteEvent(String data) { this.data = data; }
        String getData() { return data; }
    }

    static class PrimaryEvent implements Event {
        private final String data;
        PrimaryEvent(String data) { this.data = data; }
        String getData() { return data; }
    }

    static class SecondaryEvent implements Event {
        private final String data;
        SecondaryEvent(String data) { this.data = data; }
        String getData() { return data; }
    }

    static class TertiaryEvent implements Event {
        private final String data;
        TertiaryEvent(String data) { this.data = data; }
        String getData() { return data; }
    }

    static class FilterableEvent implements Event {
        private final String data;
        private final boolean shouldProcess;
        FilterableEvent(String data, boolean shouldProcess) {
            this.data = data;
            this.shouldProcess = shouldProcess;
        }
        String getData() { return data; }
        boolean shouldProcess() { return shouldProcess; }
    }

    static class AggregatorEvent implements Event {
        private final List<Integer> values = new ArrayList<>();
        void addValue(int value) { values.add(value); }
        List<Integer> getValues() { return values; }
        int getTotal() { return values.stream().mapToInt(Integer::intValue).sum(); }
    }

    static class TransactionEvent implements CancellableEvent {
        private String data = "";
        private boolean cancelled = false;
        String getData() { return data; }
        void setData(String data) { this.data = data; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    }

    static class ObservableEvent implements Event {
        private final String data;
        ObservableEvent(String data) { this.data = data; }
        String getData() { return data; }
    }
}

