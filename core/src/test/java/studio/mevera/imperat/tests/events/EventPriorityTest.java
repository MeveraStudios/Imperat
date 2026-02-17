package studio.mevera.imperat.tests.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.util.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests handler priority ordering and execution.
 */
@DisplayName("EventBus - Priority Tests")
public class EventPriorityTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.createDummy();
    }

    @Test
    @DisplayName("Should execute handlers in priority order (HIGHEST to LOWEST)")
    void testPriorityOrder() {
        List<Priority> executionOrder = new ArrayList<>();

        eventBus.register(TestEvent.class, e -> executionOrder.add(Priority.MINIMUM), Priority.MINIMUM);
        eventBus.register(TestEvent.class, e -> executionOrder.add(Priority.LOW), Priority.LOW);
        eventBus.register(TestEvent.class, e -> executionOrder.add(Priority.NORMAL), Priority.NORMAL);
        eventBus.register(TestEvent.class, e -> executionOrder.add(Priority.HIGH), Priority.HIGH);
        eventBus.register(TestEvent.class, e -> executionOrder.add(Priority.MAXIMUM), Priority.MAXIMUM);

        eventBus.post(new TestEvent("test"));

        assertEquals(
            List.of(Priority.MAXIMUM, Priority.HIGH, Priority.NORMAL, Priority.LOW, Priority.MINIMUM),
            executionOrder,
            "Handlers should execute in priority order"
        );
    }

    @Test
    @DisplayName("Should execute same priority handlers in registration order")
    void testSamePriorityRegistrationOrder() {
        List<Integer> executionOrder = new ArrayList<>();

        eventBus.register(TestEvent.class, e -> executionOrder.add(1), Priority.NORMAL);
        eventBus.register(TestEvent.class, e -> executionOrder.add(2), Priority.NORMAL);
        eventBus.register(TestEvent.class, e -> executionOrder.add(3), Priority.NORMAL);

        eventBus.post(new TestEvent("test"));

        assertEquals(List.of(1, 2, 3), executionOrder, "Same priority handlers should execute in registration order");
    }

    @Test
    @DisplayName("Should respect priority across multiple event types")
    void testPriorityAcrossEventTypes() {
        List<String> executionOrder = new ArrayList<>();

        eventBus.register(TestEvent.class, e -> executionOrder.add("TestEvent-HIGH"), Priority.HIGH);
        eventBus.register(TestEvent.class, e -> executionOrder.add("TestEvent-LOW"), Priority.LOW);
        eventBus.register(OtherTestEvent.class, e -> executionOrder.add("OtherTestEvent-HIGH"), Priority.HIGH);
        eventBus.register(OtherTestEvent.class, e -> executionOrder.add("OtherTestEvent-LOW"), Priority.LOW);

        eventBus.post(new TestEvent("test"));
        assertEquals(List.of("TestEvent-HIGH", "TestEvent-LOW"), executionOrder);

        executionOrder.clear();
        eventBus.post(new OtherTestEvent("other"));
        assertEquals(List.of("OtherTestEvent-HIGH", "OtherTestEvent-LOW"), executionOrder);
    }

    @Test
    @DisplayName("Should allow high priority handler to modify data before low priority")
    void testDataModificationByPriority() {
        MutableTestEvent event = new MutableTestEvent(0);

        eventBus.register(MutableTestEvent.class, e -> e.setValue(e.getValue() + 1), Priority.MAXIMUM);
        eventBus.register(MutableTestEvent.class, e -> e.setValue(e.getValue() * 2), Priority.HIGH);
        eventBus.register(MutableTestEvent.class, e -> e.setValue(e.getValue() + 10), Priority.NORMAL);

        eventBus.post(event);

        // (0 + 1) * 2 + 10 = 12
        assertEquals(12, event.getValue(), "Handlers should modify value in priority order");
    }

    @Test
    @DisplayName("Should default to NORMAL priority when not specified")
    void testDefaultPriority() {
        List<String> executionOrder = new ArrayList<>();

        eventBus.register(TestEvent.class, e -> executionOrder.add("HIGH"), Priority.HIGH);
        eventBus.register(TestEvent.class, e -> executionOrder.add("DEFAULT")); // Uses default priority
        eventBus.register(TestEvent.class, e -> executionOrder.add("LOW"), Priority.LOW);

        eventBus.post(new TestEvent("test"));

        // Default should be NORMAL, so it executes between HIGH and LOW
        assertEquals(List.of("HIGH", "DEFAULT", "LOW"), executionOrder);
    }

    @Test
    @DisplayName("Should handle priority with validation pattern")
    void testPriorityValidationPattern() {
        List<String> actions = new ArrayList<>();
        MutableTestEvent event = new MutableTestEvent(5);

        // Validation runs first
        eventBus.register(MutableTestEvent.class, e -> {
            actions.add("validate");
            if (e.getValue() < 0) {
                e.setValue(0);
            }
        }, Priority.MAXIMUM);

        // Business logic runs second
        eventBus.register(MutableTestEvent.class, e -> {
            actions.add("process");
            e.setValue(e.getValue() * 2);
        }, Priority.NORMAL);

        // Logging runs last
        eventBus.register(MutableTestEvent.class, e -> {
            actions.add("log");
        }, Priority.MINIMUM);

        eventBus.post(event);

        assertEquals(List.of("validate", "process", "log"), actions);
        assertEquals(10, event.getValue());
    }

    @Test
    @DisplayName("Should handle mixed priority with multiple handlers")
    void testComplexPriorityScenario() {
        List<String> executionOrder = new ArrayList<>();

        eventBus.register(TestEvent.class, e -> executionOrder.add("1-HIGHEST"), Priority.MAXIMUM);
        eventBus.register(TestEvent.class, e -> executionOrder.add("2-HIGHEST"), Priority.MAXIMUM);
        eventBus.register(TestEvent.class, e -> executionOrder.add("1-HIGH"), Priority.HIGH);
        eventBus.register(TestEvent.class, e -> executionOrder.add("1-NORMAL"), Priority.NORMAL);
        eventBus.register(TestEvent.class, e -> executionOrder.add("2-NORMAL"), Priority.NORMAL);
        eventBus.register(TestEvent.class, e -> executionOrder.add("1-LOW"), Priority.LOW);
        eventBus.register(TestEvent.class, e -> executionOrder.add("1-LOWEST"), Priority.MINIMUM);

        eventBus.post(new TestEvent("test"));

        assertEquals(
            List.of(
                "1-HIGHEST", "2-HIGHEST",  // HIGHEST in registration order
                "1-HIGH",                   // HIGH
                "1-NORMAL", "2-NORMAL",    // NORMAL in registration order
                "1-LOW",                    // LOW
                "1-LOWEST"                  // LOWEST
            ),
            executionOrder
        );
    }

    @Test
    @DisplayName("Should maintain priority after unregister and re-register")
    void testPriorityAfterReRegistration() {
        List<String> executionOrder = new ArrayList<>();

        var sub1 = eventBus.register(TestEvent.class, e -> executionOrder.add("FIRST-HIGH"), Priority.HIGH);
        eventBus.register(TestEvent.class, e -> executionOrder.add("LOW"), Priority.LOW);

        eventBus.unregister(sub1.id());
        eventBus.register(TestEvent.class, e -> executionOrder.add("SECOND-HIGH"), Priority.HIGH);

        eventBus.post(new TestEvent("test"));

        assertEquals(List.of("SECOND-HIGH", "LOW"), executionOrder);
    }

    @Test
    @DisplayName("Should handle priority with accumulation pattern")
    void testPriorityAccumulation() {
        AtomicInteger accumulator = new AtomicInteger(0);

        // All handlers add to accumulator
        eventBus.register(TestEvent.class, e -> accumulator.addAndGet(1), Priority.MAXIMUM);
        eventBus.register(TestEvent.class, e -> accumulator.addAndGet(10), Priority.HIGH);
        eventBus.register(TestEvent.class, e -> accumulator.addAndGet(100), Priority.NORMAL);
        eventBus.register(TestEvent.class, e -> accumulator.addAndGet(1000), Priority.LOW);

        eventBus.post(new TestEvent("test"));

        assertEquals(1111, accumulator.get(), "All handlers should contribute in order");
    }

    // Test event classes
    static class TestEvent implements Event {
        private final String data;

        TestEvent(String data) {
            this.data = data;
        }
    }

    static class OtherTestEvent implements Event {
        private final String data;

        OtherTestEvent(String data) {
            this.data = data;
        }
    }

    static class MutableTestEvent implements Event {
        private int value;

        MutableTestEvent(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }

        void setValue(int value) {
            this.value = value;
        }
    }
}

