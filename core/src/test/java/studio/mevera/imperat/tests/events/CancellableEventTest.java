package studio.mevera.imperat.tests.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.events.CancellableEvent;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.util.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests cancellable event functionality.
 */
@DisplayName("EventBus - Cancellable Events Tests")
public class CancellableEventTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.createDummy();
    }

    @Test
    @DisplayName("Should create cancellable event with initial state false")
    void testCancellableEventInitialState() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        assertFalse(event.isCancelled(), "Event should not be cancelled initially");
    }

    @Test
    @DisplayName("Should allow cancelling event")
    void testEventCancellation() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        event.setCancelled(true);
        assertTrue(event.isCancelled(), "Event should be cancelled");
    }

    @Test
    @DisplayName("Should allow un-cancelling event")
    void testEventUnCancellation() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        event.setCancelled(true);
        event.setCancelled(false);
        assertFalse(event.isCancelled(), "Event should not be cancelled after un-cancelling");
    }

    @Test
    @DisplayName("Should propagate cancellation status to handlers")
    void testCancellationPropagation() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        AtomicBoolean sawCancelled = new AtomicBoolean(false);

        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(true);
        }, Priority.HIGH);

        eventBus.register(TestCancellableEvent.class, e -> {
            if (e.isCancelled()) {
                sawCancelled.set(true);
            }
        }, Priority.NORMAL);

        eventBus.post(event);

        assertTrue(sawCancelled.get(), "Second handler should see cancellation");
        assertTrue(event.isCancelled(), "Event should remain cancelled");
    }

    @Test
    @DisplayName("Should allow handlers to respect cancellation")
    void testHandlerRespectsCancellation() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        AtomicBoolean actionPerformed = new AtomicBoolean(false);

        // First handler cancels
        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(true);
        }, Priority.HIGH);

        // Second handler respects cancellation
        eventBus.register(TestCancellableEvent.class, e -> {
            if (!e.isCancelled()) {
                actionPerformed.set(true);
            }
        }, Priority.NORMAL);

        eventBus.post(event);

        assertFalse(actionPerformed.get(), "Action should not be performed when event is cancelled");
    }

    @Test
    @DisplayName("Should allow handlers to override cancellation")
    void testCancellationOverride() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        List<String> actions = new ArrayList<>();

        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(true);
            actions.add("cancelled");
        }, Priority.HIGH);

        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(false); // Admin override
            actions.add("uncancelled");
        }, Priority.NORMAL);

        eventBus.register(TestCancellableEvent.class, e -> {
            if (!e.isCancelled()) {
                actions.add("executed");
            }
        }, Priority.LOW);

        eventBus.post(event);

        assertEquals(List.of("cancelled", "uncancelled", "executed"), actions);
        assertFalse(event.isCancelled(), "Event should not be cancelled after override");
    }

    @Test
    @DisplayName("Should still invoke all handlers even when cancelled")
    void testAllHandlersInvokedWhenCancelled() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        List<Integer> callOrder = new ArrayList<>();

        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(true);
            callOrder.add(1);
        }, Priority.HIGH);

        eventBus.register(TestCancellableEvent.class, e -> {
            callOrder.add(2);
        }, Priority.NORMAL);

        eventBus.register(TestCancellableEvent.class, e -> {
            callOrder.add(3);
        }, Priority.LOW);

        eventBus.post(event);

        assertEquals(List.of(1, 2, 3), callOrder, "All handlers should be invoked even when cancelled");
    }

    @Test
    @DisplayName("Should handle validation pattern correctly")
    void testValidationPattern() {
        TestCancellableEvent event = new TestCancellableEvent("forbidden");
        AtomicBoolean validationRan = new AtomicBoolean(false);
        AtomicBoolean actionRan = new AtomicBoolean(false);

        // Validator
        eventBus.register(TestCancellableEvent.class, e -> {
            validationRan.set(true);
            if (e.getData().equals("forbidden")) {
                e.setCancelled(true);
            }
        }, Priority.MAXIMUM);

        // Action
        eventBus.register(TestCancellableEvent.class, e -> {
            if (!e.isCancelled()) {
                actionRan.set(true);
            }
        }, Priority.NORMAL);

        eventBus.post(event);

        assertTrue(validationRan.get(), "Validation should run");
        assertFalse(actionRan.get(), "Action should not run when validation fails");
        assertTrue(event.isCancelled(), "Event should be cancelled");
    }

    @Test
    @DisplayName("Should allow logging of cancelled events")
    void testCancelledEventLogging() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        List<String> logs = new ArrayList<>();

        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(true);
        }, Priority.HIGH);

        // Logger that runs even for cancelled events
        eventBus.register(TestCancellableEvent.class, e -> {
            if (e.isCancelled()) {
                logs.add("Event was cancelled");
            } else {
                logs.add("Event was not cancelled");
            }
        }, Priority.MINIMUM);

        eventBus.post(event);

        assertTrue(logs.contains("Event was cancelled"), "Logger should record cancellation");
    }

    @Test
    @DisplayName("Should handle multiple cancellation toggles")
    void testMultipleCancellationToggles() {
        TestCancellableEvent event = new TestCancellableEvent("test");
        List<Boolean> states = new ArrayList<>();

        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(true);
            states.add(e.isCancelled());
        }, Priority.MAXIMUM);

        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(false);
            states.add(e.isCancelled());
        }, Priority.HIGH);

        eventBus.register(TestCancellableEvent.class, e -> {
            e.setCancelled(true);
            states.add(e.isCancelled());
        }, Priority.NORMAL);

        eventBus.post(event);

        assertEquals(List.of(true, false, true), states, "Should track all cancellation state changes");
    }

    // Test event class
    static class TestCancellableEvent implements CancellableEvent {
        private final String data;
        private boolean cancelled = false;

        TestCancellableEvent(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}

