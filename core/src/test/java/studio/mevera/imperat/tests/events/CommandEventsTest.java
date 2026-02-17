package studio.mevera.imperat.tests.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.events.types.CommandPostRegistrationEvent;
import studio.mevera.imperat.events.types.CommandPreRegistrationEvent;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.util.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests command-specific events (PreRegistration and PostRegistration).
 */
@DisplayName("EventBus - Command Events Tests")
public class CommandEventsTest {

    private EventBus eventBus;
    private Command<TestSource> testCommand;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.createDummy();
        testCommand = Command.create(ImperatTestGlobals.IMPERAT, "testcommand")
                              .build();
    }

    @Test
    @DisplayName("Should fire CommandPreRegistrationEvent")
    void testPreRegistrationEventFired() {
        AtomicBoolean eventFired = new AtomicBoolean(false);

        eventBus.register(CommandPreRegistrationEvent.class, event -> {
            eventFired.set(true);
        });

        eventBus.post(new CommandPreRegistrationEvent<>(testCommand));

        assertTrue(eventFired.get(), "Pre-registration event should be fired");
    }

    @Test
    @DisplayName("Should provide correct command in PreRegistrationEvent")
    void testPreRegistrationEventCommand() {
        AtomicReference<Command<TestSource>> receivedCommand = new AtomicReference<>();

        eventBus.register(CommandPreRegistrationEvent.class, event -> {
            receivedCommand.set(event.getCommand());
        });

        eventBus.post(new CommandPreRegistrationEvent<>(testCommand));

        assertSame(testCommand, receivedCommand.get(), "Event should contain correct command");
    }

    @Test
    @DisplayName("Should allow cancelling command registration")
    void testCancelRegistration() {
        CommandPreRegistrationEvent<TestSource> event = new CommandPreRegistrationEvent<>(testCommand);

        eventBus.register(CommandPreRegistrationEvent.class, e -> {
            e.setCancelled(true);
        });

        eventBus.post(event);

        assertTrue(event.isCancelled(), "Event should be cancelled");
    }

    @Test
    @DisplayName("Should check cancellation status before registration")
    void testCancellationBeforeRegistration() {
        List<String> actions = new ArrayList<>();
        CommandPreRegistrationEvent<TestSource> event = new CommandPreRegistrationEvent<>(testCommand);

        eventBus.register(CommandPreRegistrationEvent.class, e -> {
            e.setCancelled(true);
            actions.add("cancelled");
        }, Priority.HIGH);

        eventBus.register(CommandPreRegistrationEvent.class, e -> {
            if (!e.isCancelled()) {
                actions.add("would-register");
            } else {
                actions.add("skip-registration");
            }
        }, Priority.LOW);

        eventBus.post(event);

        assertEquals(List.of("cancelled", "skip-registration"), actions);
    }

    @Test
    @DisplayName("Should fire CommandPostRegistrationEvent on success")
    void testPostRegistrationEventSuccess() {
        AtomicBoolean eventFired = new AtomicBoolean(false);
        AtomicBoolean wasSuccessful = new AtomicBoolean(false);

        eventBus.register(CommandPostRegistrationEvent.class, event -> {
            eventFired.set(true);
            wasSuccessful.set(event.isSuccessful());
        });

        eventBus.post(new CommandPostRegistrationEvent<>(testCommand, null));

        assertTrue(eventFired.get(), "Post-registration event should be fired");
        assertTrue(wasSuccessful.get(), "Event should indicate success");
    }

    @Test
    @DisplayName("Should fire CommandPostRegistrationEvent on failure")
    void testPostRegistrationEventFailure() {
        RuntimeException failure = new RuntimeException("Registration failed");
        AtomicBoolean eventFired = new AtomicBoolean(false);
        AtomicBoolean wasFailed = new AtomicBoolean(false);
        AtomicReference<Throwable> receivedCause = new AtomicReference<>();

        eventBus.register(CommandPostRegistrationEvent.class, event -> {
            eventFired.set(true);
            wasFailed.set(event.isFailed());
            receivedCause.set(event.getFailureCause());
        });

        eventBus.post(new CommandPostRegistrationEvent<>(testCommand, failure));

        assertTrue(eventFired.get(), "Post-registration event should be fired");
        assertTrue(wasFailed.get(), "Event should indicate failure");
        assertSame(failure, receivedCause.get(), "Event should contain failure cause");
    }

    @Test
    @DisplayName("Should indicate success when no failure cause")
    void testSuccessIndicator() {
        CommandPostRegistrationEvent<TestSource> event =
            new CommandPostRegistrationEvent<>(testCommand, null);

        assertTrue(event.isSuccessful());
        assertFalse(event.isFailed());
        assertNull(event.getFailureCause());
    }

    @Test
    @DisplayName("Should indicate failure when failure cause present")
    void testFailureIndicator() {
        RuntimeException cause = new RuntimeException("Error");
        CommandPostRegistrationEvent<TestSource> event =
            new CommandPostRegistrationEvent<>(testCommand, cause);

        assertFalse(event.isSuccessful());
        assertTrue(event.isFailed());
        assertSame(cause, event.getFailureCause());
    }

    @Test
    @DisplayName("Should handle complete registration lifecycle")
    void testCompleteRegistrationLifecycle() {
        List<String> lifecycle = new ArrayList<>();

        eventBus.register(CommandPreRegistrationEvent.class, event -> {
            lifecycle.add("pre-registration");
            assertFalse(event.isCancelled());
        });

        eventBus.register(CommandPostRegistrationEvent.class, event -> {
            lifecycle.add("post-registration");
            assertTrue(event.isSuccessful());
        });

        // Simulate registration lifecycle
        CommandPreRegistrationEvent<TestSource> preEvent =
            new CommandPreRegistrationEvent<>(testCommand);
        eventBus.post(preEvent);

        if (!preEvent.isCancelled()) {
            // Registration would happen here
            CommandPostRegistrationEvent<TestSource> postEvent =
                new CommandPostRegistrationEvent<>(testCommand, null);
            eventBus.post(postEvent);
        }

        assertEquals(List.of("pre-registration", "post-registration"), lifecycle);
    }

    @Test
    @DisplayName("Should handle cancelled registration lifecycle")
    void testCancelledRegistrationLifecycle() {
        List<String> lifecycle = new ArrayList<>();

        eventBus.register(CommandPreRegistrationEvent.class, event -> {
            lifecycle.add("pre-registration");
            if (event.getCommand().name().equals("testcommand")) {
                event.setCancelled(true);
                lifecycle.add("cancelled");
            }
        });

        eventBus.register(CommandPostRegistrationEvent.class, event -> {
            lifecycle.add("post-registration");
            if (event.isFailed()) {
                lifecycle.add("registration-failed");
            }
        });

        // Simulate cancelled registration
        CommandPreRegistrationEvent<TestSource> preEvent =
            new CommandPreRegistrationEvent<>(testCommand);
        eventBus.post(preEvent);

        if (preEvent.isCancelled()) {
            // Registration blocked, fire failure event
            CommandPostRegistrationEvent<TestSource> postEvent =
                new CommandPostRegistrationEvent<>(testCommand,
                    new IllegalStateException("Registration cancelled"));
            eventBus.post(postEvent);
        }

        assertEquals(List.of("pre-registration", "cancelled", "post-registration", "registration-failed"),
                    lifecycle);
    }

    @Test
    @DisplayName("Should allow multiple validators in PreRegistration")
    void testMultipleValidators() {
        AtomicInteger validatorCount = new AtomicInteger(0);
        CommandPreRegistrationEvent<TestSource> event = new CommandPreRegistrationEvent<>(testCommand);

        // Validator 1
        eventBus.register(CommandPreRegistrationEvent.class, e -> {
            validatorCount.incrementAndGet();
        }, Priority.MAXIMUM);

        // Validator 2
        eventBus.register(CommandPreRegistrationEvent.class, e -> {
            validatorCount.incrementAndGet();
        }, Priority.HIGH);

        // Validator 3 that cancels
        eventBus.register(CommandPreRegistrationEvent.class, e -> {
            validatorCount.incrementAndGet();
            e.setCancelled(true);
        }, Priority.NORMAL);

        eventBus.post(event);

        assertEquals(3, validatorCount.get(), "All validators should run");
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("Should handle PostRegistration logging")
    void testPostRegistrationLogging() {
        List<String> logs = new ArrayList<>();

        eventBus.register(CommandPostRegistrationEvent.class, event -> {
            if (event.isSuccessful()) {
                logs.add("SUCCESS: " + event.getCommand().name());
            } else {
                logs.add("FAILURE: " + event.getCommand().name() +
                        " - " + event.getFailureCause().getMessage());
            }
        });

        // Test success
        eventBus.post(new CommandPostRegistrationEvent<>(testCommand, null));

        // Test failure
        Command<TestSource> failedCommand = Command.create(ImperatTestGlobals.IMPERAT, "failedcommand")
                                                    .build();
        eventBus.post(new CommandPostRegistrationEvent<>(failedCommand,
            new RuntimeException("Conflict")));

        assertEquals(2, logs.size());
        assertTrue(logs.get(0).startsWith("SUCCESS:"));
        assertTrue(logs.get(1).startsWith("FAILURE:"));
    }

    @Test
    @DisplayName("Should maintain command reference through events")
    void testCommandReferenceConsistency() {
        AtomicReference<Command<TestSource>> preCommand = new AtomicReference<>();
        AtomicReference<Command<TestSource>> postCommand = new AtomicReference<>();

        eventBus.register(CommandPreRegistrationEvent.class, event -> {
            preCommand.set(event.getCommand());
        });

        eventBus.register(CommandPostRegistrationEvent.class, event -> {
            postCommand.set(event.getCommand());
        });

        eventBus.post(new CommandPreRegistrationEvent<>(testCommand));
        eventBus.post(new CommandPostRegistrationEvent<>(testCommand, null));

        assertSame(testCommand, preCommand.get());
        assertSame(testCommand, postCommand.get());
        assertSame(preCommand.get(), postCommand.get());
    }

    @Test
    @DisplayName("Should support conditional registration based on command properties")
    void testConditionalRegistration() {
        Command<TestSource> adminCommand = Command.create(ImperatTestGlobals.IMPERAT, "admin").build();
        Command<TestSource> userCommand = Command.create(ImperatTestGlobals.IMPERAT, "user").build();

        List<String> registeredCommands = new ArrayList<>();

        eventBus.register(CommandPreRegistrationEvent.class, event -> {
            Command<?> cmd = event.getCommand();
            if (cmd.name().startsWith("admin")) {
                // Simulate permission check
                event.setCancelled(true);
            }
        });

        eventBus.register(CommandPostRegistrationEvent.class, event -> {
            if (event.isSuccessful()) {
                registeredCommands.add(event.getCommand().name());
            }
        });

        // Simulate registration attempts
        CommandPreRegistrationEvent<TestSource> adminPreEvent =
            new CommandPreRegistrationEvent<>(adminCommand);
        eventBus.post(adminPreEvent);
        if (!adminPreEvent.isCancelled()) {
            eventBus.post(new CommandPostRegistrationEvent<>(adminCommand, null));
        }

        CommandPreRegistrationEvent<TestSource> userPreEvent =
            new CommandPreRegistrationEvent<>(userCommand);
        eventBus.post(userPreEvent);
        if (!userPreEvent.isCancelled()) {
            eventBus.post(new CommandPostRegistrationEvent<>(userCommand, null));
        }

        assertEquals(List.of("user"), registeredCommands);
    }
}

