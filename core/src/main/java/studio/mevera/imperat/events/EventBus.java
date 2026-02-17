package studio.mevera.imperat.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.util.Priority;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * A high-performance, thread-safe event bus system with priority-based handler execution.
 *
 * <p>The EventBus allows registration of type-safe event handlers that are invoked when
 * events are posted. Handlers can execute synchronously or asynchronously, with configurable
 * priority levels determining execution order.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *     <li><strong>Type Safety:</strong> Only events implementing {@link CommandEvent} are allowed</li>
 *     <li><strong>Strict Type Matching:</strong> Handlers receive only exact event type matches (no inheritance)</li>
 *     <li><strong>Thread Safety:</strong> Fully concurrent for handler registration and event posting</li>
 *     <li><strong>Priority-Based Execution:</strong> Handlers execute in priority order (highest first)</li>
 *     <li><strong>Sync/Async Execution:</strong> Per-handler control over execution strategy</li>
 *     <li><strong>Exception Isolation:</strong> Handler failures don't stop other handlers</li>
 *     <li><strong>Event Cancellation:</strong> Support for {@link CancellableEvent}</li>
 *     <li><strong>Handler Unregistration:</strong> Remove subscriptions by UUID</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * EventBus eventBus = EventBus.create();
 *
 * UUID handlerId = eventBus.register(
 *     PlayerJoinEvent.class,
 *     event -> System.out.println("Player joined: " + event.getPlayer())
 * );
 *
 * eventBus.post(new PlayerJoinEvent(player));
 * eventBus.unregister(handlerId);
 * }</pre>
 *
 * <h2>Advanced Usage</h2>
 * <pre>{@code
 * EventBus eventBus = EventBus.builder()
 *     .exceptionHandler((event, exception, handlerId) ->
 *         logger.error("Handler {} failed", handlerId, exception))
 *     .executorService(customExecutor)
 *     .build();
 *
 * eventBus.register(
 *     PlayerChatEvent.class,
 *     event -> {
 *         if (!event.isCancelled()) broadcastMessage(event.getMessage());
 *     },
 *     Priority.HIGH,
 *     ExecutionStrategy.ASYNC
 * );
 * }</pre>
 *
 * <h2>Execution Order</h2>
 * <p>When an event is posted:</p>
 * <ol>
 *     <li>All SYNC subscriptions execute in priority order (highest to lowest)</li>
 *     <li>All ASYNC subscriptions are submitted in priority order (highest to lowest)</li>
 *     <li>post() returns after all SYNC subscriptions complete (ASYNC may still be running)</li>
 * </ol>
 *
 * <h2>Exception Handling</h2>
 * <p>When a handler throws an exception:</p>
 * <ol>
 *     <li>The configured {@link EventExceptionHandler} is invoked with full context</li>
 *     <li>Execution continues to the next subscription (isolation)</li>
 *     <li>Other subscriptions are not affected by the failure</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *     <li>Multiple threads can post events concurrently</li>
 *     <li>Subscriptions can be registered/unregistered while events are being posted</li>
 *     <li>Internal state uses concurrent data structures</li>
 * </ul>
 *
 * @see CommandEvent
 * @see CancellableEvent
 * @see EventSubscription
 * @see EventExceptionHandler
 * @see ExecutionStrategy
 */
public interface EventBus {

    /**
     * Creates a new EventBus with default configuration.
     *
     * <p>Default configuration:</p>
     * <ul>
     *     <li>No exception handler (exceptions are silently ignored)</li>
     *     <li>Default daemon cached thread pool for async handlers</li>
     * </ul>
     *
     * @return a new EventBus instance
     */
    static EventBus createDummy() {
        return new EventBusImpl(null, null);
    }

    /**
     * Returns a new {@link Builder} for configuring an EventBus.
     *
     * @return a new Builder instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Registers an event handler with {@link Priority#NORMAL} and {@link ExecutionStrategy#SYNC}.
     *
     * @param eventType the exact class of events this handler should receive
     * @param handler   the consumer that will process events
     * @param <T>       the type of event
     * @return the unique identifier of this subscription, usable with {@link #unregister(UUID)}
     * @throws IllegalArgumentException if any parameter is null
     */
    <T extends Event> EventSubscription<T> register(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    );

    /**
     * Registers an event handler with the specified priority and {@link ExecutionStrategy#SYNC}.
     *
     * @param eventType the exact class of events this handler should receive
     * @param handler   the consumer that will process events
     * @param priority  the execution priority for this handler
     * @param <T>       the type of event
     * @return the unique identifier of this subscription, usable with {@link #unregister(UUID)}
     * @throws IllegalArgumentException if any parameter is null
     */
    <T extends Event> EventSubscription<T> register(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @NotNull Priority priority
    );

    /**
     * Registers an event handler with full configuration.
     *
     * <p>Handlers are invoked only for events whose runtime type exactly matches
     * {@code eventType}. Inheritance is not considered — a handler for
     * {@code CommandEvent.class} will NOT receive {@code PlayerJoinEvent} instances.</p>
     *
     * <p>Handlers with equal priority execute in registration order (FIFO).</p>
     *
     * @param eventType the exact class of events this handler should receive
     * @param handler   the consumer that will process events
     * @param priority  the execution priority for this handler
     * @param strategy  whether to execute synchronously or asynchronously
     * @param <T>       the type of event
     * @return the unique identifier of this subscription, usable with {@link #unregister(UUID)}
     * @throws IllegalArgumentException if any parameter is null
     */
    <T extends Event> EventSubscription<T> register(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @NotNull Priority priority,
            @NotNull ExecutionStrategy strategy
    );

    /**
     * Unregisters a subscription by its unique identifier.
     *
     * <p>If the handler is currently executing, this does not interrupt it.
     * The subscription will simply not receive any future events.</p>
     *
     * @param subscriptionId the ID returned from a prior {@link #register} call
     * @return true if the subscription was found and removed, false if not found
     */
    boolean unregister(@NotNull UUID subscriptionId);

    /**
     * Posts an event to all subscriptions registered for its exact runtime type.
     *
     * <p>SYNC subscriptions execute in the calling thread in priority order before
     * this method returns. ASYNC subscriptions are submitted to the internal executor
     * and may still be running after this method returns.</p>
     *
     * @param event the event to post
     * @param <T>   the type of event
     * @throws IllegalArgumentException if event is null
     */
    <T extends Event> void post(@NotNull T event);

    /**
     * Returns the number of active subscriptions for the given exact event type.
     *
     * @param eventType the event type to query
     * @return the subscription count, or 0 if none are registered
     */
    int getSubscriptionCount(@NotNull Class<? extends Event> eventType);

    /**
     * Returns the total number of active subscriptions across all event types.
     *
     * @return the total subscription count
     */
    int getTotalSubscriptionCount();

    /**
     * Shuts down the event bus, releasing resources.
     *
     * <p>If the EventBus owns its {@link ExecutorService} (default), it will be shut down.
     * If a custom one was supplied via the {@link Builder}, it will NOT be shut down —
     * the caller is responsible for its lifecycle.</p>
     *
     * <p>This method does not block. Use {@link #shutdownAndWait()} to wait for
     * all currently running async handlers to finish before returning.</p>
     */
    void shutdown();

    /**
     * Shuts down the event bus and blocks until all async handlers finish executing.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    void shutdownAndWait() throws InterruptedException;

    /**
     * Builder for creating configured {@link EventBus} instances.
     *
     * <pre>{@code
     * EventBus eventBus = EventBus.builder()
     *     .exceptionHandler((event, ex, id) -> logger.error("Handler failed", ex))
     *     .executorService(myCustomExecutor)
     *     .build();
     * }</pre>
     */
    final class Builder {

        private EventExceptionHandler exceptionHandler;
        private ExecutorService executorService;

        private Builder() {}

        /**
         * Sets the global exception handler.
         *
         * <p>Invoked with full context whenever any registered handler throws an exception.
         * Optional — if not set, exceptions are silently ignored.</p>
         *
         * @param handler the exception handler, or null to leave unset
         * @return this builder
         */
        public Builder exceptionHandler(@Nullable EventExceptionHandler handler) {
            this.exceptionHandler = handler;
            return this;
        }

        /**
         * Sets a custom {@link ExecutorService} for async handler execution.
         *
         * <p>If not set, a default daemon cached thread pool is created and owned by the bus.
         * If you supply one here, you are responsible for shutting it down independently.</p>
         *
         * @param executorService the executor service, or null to use the default
         * @return this builder
         */
        public Builder executorService(@Nullable ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        /**
         * Builds and returns a configured {@link EventBus}.
         *
         * @return a new EventBus instance
         */
        public EventBus build() {
            return new EventBusImpl(exceptionHandler, executorService);
        }
    }

    /**
     * Checks if this instance of event bus has no executor and no exception handler configured.
     * @return true if this is a dummy bus, false otherwise
     */
    boolean isDummyBus();
}