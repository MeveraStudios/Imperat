package studio.mevera.imperat.tests.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.events.EventSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests basic EventBus functionality including registration, posting, and unregistration.
 */
@DisplayName("EventBus - Basic Functionality Tests")
public class EventBusBasicTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.createDummy();
    }

    @Test
    @DisplayName("Should create EventBus successfully")
    void testEventBusCreation() {
        assertNotNull(eventBus, "EventBus should be created");
        EventBus customBus = EventBus.builder().build();
        assertNotNull(customBus, "EventBus from builder should be created");
    }

    @Test
    @DisplayName("Should register and invoke handler")
    void testBasicHandlerRegistration() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        TestEvent event = new TestEvent("test");

        eventBus.register(TestEvent.class, e -> handlerCalled.set(true));
        eventBus.post(event);

        assertTrue(handlerCalled.get(), "Handler should be invoked");
    }

    @Test
    @DisplayName("Should return subscription on registration")
    void testRegistrationReturnsSubscription() {
        EventSubscription<TestEvent> subscription =
            eventBus.register(TestEvent.class, e -> {});

        assertNotNull(subscription, "Registration should return subscription");
        assertNotNull(subscription.id(), "Subscription should have ID");
        assertNotNull(subscription.handler(), "Subscription should have handler");
        assertNotNull(subscription.priority(), "Subscription should have priority");
        assertNotNull(subscription.strategy(), "Subscription should have strategy");
    }

    @Test
    @DisplayName("Should invoke handler with correct event data")
    void testHandlerReceivesCorrectEvent() {
        TestEvent sentEvent = new TestEvent("test-data");
        AtomicBoolean correctData = new AtomicBoolean(false);

        eventBus.register(TestEvent.class, receivedEvent -> {
            if (receivedEvent.getData().equals("test-data")) {
                correctData.set(true);
            }
        });

        eventBus.post(sentEvent);
        assertTrue(correctData.get(), "Handler should receive correct event data");
    }

    @Test
    @DisplayName("Should invoke multiple handlers for same event")
    void testMultipleHandlers() {
        AtomicInteger callCount = new AtomicInteger(0);
        TestEvent event = new TestEvent("test");

        eventBus.register(TestEvent.class, e -> callCount.incrementAndGet());
        eventBus.register(TestEvent.class, e -> callCount.incrementAndGet());
        eventBus.register(TestEvent.class, e -> callCount.incrementAndGet());

        eventBus.post(event);
        assertEquals(3, callCount.get(), "All three handlers should be invoked");
    }

    @Test
    @DisplayName("Should not invoke handlers for different event types")
    void testEventTypeIsolation() {
        AtomicBoolean testEventCalled = new AtomicBoolean(false);
        AtomicBoolean otherEventCalled = new AtomicBoolean(false);

        eventBus.register(TestEvent.class, e -> testEventCalled.set(true));
        eventBus.register(OtherTestEvent.class, e -> otherEventCalled.set(true));

        eventBus.post(new TestEvent("test"));

        assertTrue(testEventCalled.get(), "TestEvent handler should be called");
        assertFalse(otherEventCalled.get(), "OtherTestEvent handler should not be called");
    }

    @Test
    @DisplayName("Should unregister handler by subscription ID")
    void testUnregisterHandler() {
        AtomicInteger callCount = new AtomicInteger(0);
        EventSubscription<TestEvent> subscription =
            eventBus.register(TestEvent.class, e -> callCount.incrementAndGet());

        eventBus.post(new TestEvent("test"));
        assertEquals(1, callCount.get(), "Handler should be called before unregister");

        eventBus.unregister(subscription.id());
        eventBus.post(new TestEvent("test"));
        assertEquals(1, callCount.get(), "Handler should not be called after unregister");
    }

    @Test
    @DisplayName("Should handle unregistering non-existent subscription gracefully")
    void testUnregisterNonExistent() {
        assertDoesNotThrow(() ->
            eventBus.unregister(java.util.UUID.randomUUID()),
            "Unregistering non-existent subscription should not throw"
        );
    }

    @Test
    @DisplayName("Should invoke handlers in registration order when priorities are equal")
    void testRegistrationOrder() {
        List<Integer> callOrder = new ArrayList<>();

        eventBus.register(TestEvent.class, e -> callOrder.add(1));
        eventBus.register(TestEvent.class, e -> callOrder.add(2));
        eventBus.register(TestEvent.class, e -> callOrder.add(3));

        eventBus.post(new TestEvent("test"));

        assertEquals(List.of(1, 2, 3), callOrder, "Handlers should be called in registration order");
    }

    @Test
    @DisplayName("Should handle posting null event")
    void testPostNullEvent() {
        assertThrows(NullPointerException.class, () ->
            eventBus.post(null),
            "Posting null event should throw NullPointerException"
        );
    }

    @Test
    @DisplayName("Should handle empty event bus posting")
    void testPostWithNoHandlers() {
        assertDoesNotThrow(() ->
            eventBus.post(new TestEvent("test")),
            "Posting to empty event bus should not throw"
        );
    }

    @Test
    @DisplayName("Should allow re-registration after unregister")
    void testReRegistrationAfterUnregister() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventSubscription<TestEvent> sub1 =
            eventBus.register(TestEvent.class, e -> callCount.incrementAndGet());
        eventBus.unregister(sub1.id());

        eventBus.register(TestEvent.class, e -> callCount.incrementAndGet());
        eventBus.post(new TestEvent("test"));

        assertEquals(1, callCount.get(), "New handler should be called after re-registration");
    }

    @Test
    @DisplayName("Should support handler registration for event subtypes")
    void testEventSubtypeHandling() {
        AtomicBoolean baseCalled = new AtomicBoolean(false);
        AtomicBoolean subCalled = new AtomicBoolean(false);

        eventBus.register(TestEvent.class, e -> baseCalled.set(true));
        eventBus.register(SpecialTestEvent.class, e -> subCalled.set(true));

        eventBus.post(new SpecialTestEvent("special"));

        assertFalse(baseCalled.get(), "Base event handler should not be called for subtype");
        assertTrue(subCalled.get(), "Subtype handler should be called");
    }

    // Test event classes
    static class TestEvent implements Event {
        private final String data;

        TestEvent(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

    static class OtherTestEvent implements Event {
        private final String data;

        OtherTestEvent(String data) {
            this.data = data;
        }
    }

    static class SpecialTestEvent extends TestEvent {
        SpecialTestEvent(String data) {
            super(data);
        }
    }
}

