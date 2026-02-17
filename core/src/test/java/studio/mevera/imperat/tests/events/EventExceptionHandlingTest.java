package studio.mevera.imperat.tests.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.events.EventExceptionHandler;
import studio.mevera.imperat.events.EventSubscription;
import studio.mevera.imperat.util.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests exception handling in event handlers.
 */
@DisplayName("EventBus - Exception Handling Tests")
public class EventExceptionHandlingTest {

    private EventBus eventBus;
    private List<ExceptionRecord> exceptionRecords;

    @BeforeEach
    void setUp() {
        exceptionRecords = Collections.synchronizedList(new ArrayList<>());

        EventExceptionHandler handler = new EventExceptionHandler() {
            @Override
            public <E extends Event> void handle(E event, Throwable exception, EventSubscription<E> subscription) {
                exceptionRecords.add(new ExceptionRecord(event, exception, subscription.id()));
            }
        };

        eventBus = EventBus.builder()
            .exceptionHandler(handler)
            .build();
    }

    @Test
    @DisplayName("Should invoke exception handler when handler throws")
    void testExceptionHandlerInvoked() {
        eventBus.register(TestEvent.class, e -> {
            throw new RuntimeException("Test exception");
        });

        eventBus.post(new TestEvent("test"));

        assertEquals(1, exceptionRecords.size(), "Exception handler should be invoked once");
        assertEquals("Test exception", exceptionRecords.get(0).exception.getMessage());
    }

    @Test
    @DisplayName("Should continue executing other handlers after exception")
    void testContinueAfterException() {
        AtomicBoolean handler1Called = new AtomicBoolean(false);
        AtomicBoolean handler2Called = new AtomicBoolean(false);
        AtomicBoolean handler3Called = new AtomicBoolean(false);

        eventBus.register(TestEvent.class, e -> {
            handler1Called.set(true);
            throw new RuntimeException("Handler 1 error");
        }, Priority.HIGH);

        eventBus.register(TestEvent.class, e -> {
            handler2Called.set(true);
        }, Priority.NORMAL);

        eventBus.register(TestEvent.class, e -> {
            handler3Called.set(true);
        }, Priority.LOW);

        eventBus.post(new TestEvent("test"));

        assertTrue(handler1Called.get(), "Handler 1 should be called");
        assertTrue(handler2Called.get(), "Handler 2 should be called after exception");
        assertTrue(handler3Called.get(), "Handler 3 should be called after exception");
        assertEquals(1, exceptionRecords.size());
    }

    @Test
    @DisplayName("Should handle multiple exceptions from different handlers")
    void testMultipleExceptions() {
        eventBus.register(TestEvent.class, e -> {
            throw new RuntimeException("Error 1");
        }, Priority.HIGH);

        eventBus.register(TestEvent.class, e -> {
            throw new IllegalStateException("Error 2");
        }, Priority.NORMAL);

        eventBus.register(TestEvent.class, e -> {
            throw new IllegalArgumentException("Error 3");
        }, Priority.LOW);

        eventBus.post(new TestEvent("test"));

        assertEquals(3, exceptionRecords.size(), "All exceptions should be recorded");
        assertTrue(exceptionRecords.stream().anyMatch(r -> r.exception.getMessage().equals("Error 1")));
        assertTrue(exceptionRecords.stream().anyMatch(r -> r.exception.getMessage().equals("Error 2")));
        assertTrue(exceptionRecords.stream().anyMatch(r -> r.exception.getMessage().equals("Error 3")));
    }

    @Test
    @DisplayName("Should provide correct event in exception handler")
    void testExceptionHandlerReceivesCorrectEvent() {
        TestEvent sentEvent = new TestEvent("test-data");

        eventBus.register(TestEvent.class, e -> {
            throw new RuntimeException("Error");
        });

        eventBus.post(sentEvent);

        assertEquals(1, exceptionRecords.size());
        assertInstanceOf(TestEvent.class, exceptionRecords.get(0).event);
        assertEquals("test-data", ((TestEvent) exceptionRecords.get(0).event).getData());
    }

    @Test
    @DisplayName("Should provide correct handler ID in exception handler")
    void testExceptionHandlerReceivesHandlerId() {
        var subscription = eventBus.register(TestEvent.class, e -> {
            throw new RuntimeException("Error");
        });

        eventBus.post(new TestEvent("test"));

        assertEquals(1, exceptionRecords.size());
        assertEquals(subscription.id(), exceptionRecords.get(0).handlerId);
    }

    @Test
    @DisplayName("Should handle different exception types")
    void testDifferentExceptionTypes() {
        eventBus.register(TestEvent.class, e -> {
            throw new NullPointerException("NPE");
        }, Priority.MAXIMUM);

        eventBus.register(TestEvent.class, e -> {
            throw new IllegalArgumentException("IAE");
        }, Priority.HIGH);

        eventBus.register(TestEvent.class, e -> {
            throw new UnsupportedOperationException("UOE");
        }, Priority.NORMAL);

        eventBus.post(new TestEvent("test"));

        assertEquals(3, exceptionRecords.size());
        assertTrue(exceptionRecords.stream().anyMatch(r -> r.exception instanceof NullPointerException));
        assertTrue(exceptionRecords.stream().anyMatch(r -> r.exception instanceof IllegalArgumentException));
        assertTrue(exceptionRecords.stream().anyMatch(r -> r.exception instanceof UnsupportedOperationException));
    }

    @Test
    @DisplayName("Should handle exception in async handler")
    void testAsyncHandlerException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean exceptionHandled = new AtomicBoolean(false);

        EventExceptionHandler handler = new EventExceptionHandler() {
            @Override public <E extends Event> void handle(E event, Throwable exception, EventSubscription<E> subscription) {
                exceptionHandled.set(true);
                latch.countDown();
            }
        };

        EventBus bus = EventBus.builder()
            .exceptionHandler(handler)
            .build();

        bus.register(TestEvent.class, e -> {
            throw new RuntimeException("Async error");
        }, Priority.NORMAL, studio.mevera.imperat.events.ExecutionStrategy.ASYNC);

        bus.post(new TestEvent("test"));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Exception handler should be called");
        assertTrue(exceptionHandled.get());
    }

    @Test
    @DisplayName("Should not throw exception back to caller")
    void testExceptionIsolation() {
        eventBus.register(TestEvent.class, e -> {
            throw new RuntimeException("Handler error");
        });

        assertDoesNotThrow(() -> eventBus.post(new TestEvent("test")),
                          "Post should not throw exception from handler");
    }

    @Test
    @DisplayName("Should handle exception with null message")
    void testExceptionWithNullMessage() {
        eventBus.register(TestEvent.class, e -> {
            throw new RuntimeException((String) null);
        });

        assertDoesNotThrow(() -> eventBus.post(new TestEvent("test")));
        assertEquals(1, exceptionRecords.size());
        assertNull(exceptionRecords.get(0).exception.getMessage());
    }

    @Test
    @DisplayName("Should handle Error types")
    void testErrorHandling() {
        eventBus.register(TestEvent.class, e -> {
            throw new AssertionError("Test error");
        });

        assertDoesNotThrow(() -> eventBus.post(new TestEvent("test")));
        assertEquals(1, exceptionRecords.size());
        assertInstanceOf(AssertionError.class, exceptionRecords.get(0).exception);
    }

    @Test
    @DisplayName("Should work without exception handler configured")
    void testNoExceptionHandler() {
        EventBus busWithoutHandler = EventBus.createDummy();

        busWithoutHandler.register(TestEvent.class, e -> {
            throw new RuntimeException("Error");
        });

        assertDoesNotThrow(() -> busWithoutHandler.post(new TestEvent("test")),
                          "Should not throw even without exception handler");
    }

    @Test
    @DisplayName("Should handle exception handler throwing exception")
    void testExceptionHandlerThrows() {
        EventExceptionHandler faultyHandler = new EventExceptionHandler() {
            @Override
            public <E extends Event> void handle(E event, Throwable exception, EventSubscription<E> subscription) {
                throw new RuntimeException("Exception handler error");
            }
        };

        EventBus bus = EventBus.builder()
            .exceptionHandler(faultyHandler)
            .build();

        bus.register(TestEvent.class, e -> {
            throw new RuntimeException("Handler error");
        });

        // Should not propagate exception from exception handler
        assertDoesNotThrow(() -> bus.post(new TestEvent("test")));
    }

    @Test
    @DisplayName("Should track exception count correctly")
    void testExceptionCounting() {
        int handlerCount = 5;
        int throwingHandlers = 3;

        for (int i = 0; i < throwingHandlers; i++) {
            eventBus.register(TestEvent.class, e -> {
                throw new RuntimeException("Error");
            });
        }

        for (int i = 0; i < (handlerCount - throwingHandlers); i++) {
            eventBus.register(TestEvent.class, e -> {
                // Normal handler
            });
        }

        eventBus.post(new TestEvent("test"));

        assertEquals(throwingHandlers, exceptionRecords.size(),
                    "Should record exactly the number of exceptions thrown");
    }

    @Test
    @DisplayName("Should handle checked exception wrapped in RuntimeException")
    void testWrappedCheckedException() {
        Exception cause = new Exception("Checked exception");

        eventBus.register(TestEvent.class, e -> {
            throw new RuntimeException("Wrapper", cause);
        });

        eventBus.post(new TestEvent("test"));

        assertEquals(1, exceptionRecords.size());
        assertEquals("Wrapper", exceptionRecords.get(0).exception.getMessage());
        assertEquals(cause, exceptionRecords.get(0).exception.getCause());
    }

    @Test
    @DisplayName("Should provide exception context for debugging")
    void testExceptionContext() {
        TestEvent event = new TestEvent("context-test");
        RuntimeException exception = new RuntimeException("Test error");

        var subscription = eventBus.register(TestEvent.class, e -> {
            throw exception;
        });

        eventBus.post(event);

        ExceptionRecord record = exceptionRecords.get(0);
        assertSame(event, record.event);
        assertEquals(exception.getMessage(), record.exception.getMessage());
        assertEquals(subscription.id(), record.handlerId);
    }

    // Test event class
    static class TestEvent implements Event {
        private final String data;

        TestEvent(String data) {
            this.data = data;
        }

        String getData() {
            return data;
        }
    }

    // Helper class to record exception details
    static class ExceptionRecord {
        final Event event;
        final Throwable exception;
        final UUID handlerId;

        ExceptionRecord(Event event, Throwable exception, UUID handlerId) {
            this.event = event;
            this.exception = exception;
            this.handlerId = handlerId;
        }
    }
}

