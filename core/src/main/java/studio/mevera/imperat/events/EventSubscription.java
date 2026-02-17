package studio.mevera.imperat.events;

import studio.mevera.imperat.util.Priority;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a single subscription to a specific event type on an {@link EventBus}.
 *
 * <p>An {@code EventSubscription} is created every time a handler is registered via
 * {@link EventBus#register} and encapsulates all the metadata associated with that
 * registration: its unique identifier, the handler itself, the priority at which it
 * executes, and whether it runs synchronously or asynchronously.</p>
 *
 * <p>The subscription's identity is solely determined by its {@link UUID}. Two
 * subscriptions with the same ID are considered equal regardless of their other fields.</p>
 *
 * <h2>Subscription Lifecycle</h2>
 * <ol>
 *     <li>Handler is registered via {@link EventBus#register(Class, Consumer)}</li>
 *     <li>EventBus creates an {@code EventSubscription} with a unique ID</li>
 *     <li>Subscription is stored and returned to the caller</li>
 *     <li>Handler is invoked whenever matching events are posted</li>
 *     <li>Subscription can be unregistered via {@link EventBus#unregister(UUID)}</li>
 * </ol>
 *
 * <h2>Example: Storing and Managing Subscriptions</h2>
 * <pre>{@code
 * // Store subscription for later unregistration
 * EventSubscription<PlayerJoinEvent> subscription =
 *     eventBus.register(PlayerJoinEvent.class, event -> {
 *         logger.info("Player joined: {}", event.getPlayer().getName());
 *     });
 *
 * // Later, unregister using the subscription ID
 * eventBus.unregister(subscription.id());
 * }</pre>
 *
 * <h2>Example: Inspecting Subscription Metadata</h2>
 * <pre>{@code
 * EventSubscription<?> subscription = eventBus.register(
 *     CommandPostRegistrationEvent.class,
 *     event -> handleCommandRegistration(event),
 *     Priority.HIGH,
 *     ExecutionStrategy.ASYNC
 * );
 *
 * logger.info("Registered handler with ID: {}", subscription.id());
 * logger.info("Priority: {}", subscription.priority());
 * logger.info("Strategy: {}", subscription.strategy());
 * }</pre>
 *
 * <h2>Example: Dynamic Handler Management</h2>
 * <pre>{@code
 * // Track subscriptions for cleanup
 * Map<String, EventSubscription<?>> activeSubscriptions = new HashMap<>();
 *
 * public void enableFeature(String featureName) {
 *     EventSubscription<?> subscription = eventBus.register(
 *         FeatureEvent.class,
 *         event -> handleFeature(event)
 *     );
 *     activeSubscriptions.put(featureName, subscription);
 * }
 *
 * public void disableFeature(String featureName) {
 *     EventSubscription<?> subscription = activeSubscriptions.remove(featureName);
 *     if (subscription != null) {
 *         eventBus.unregister(subscription.id());
 *     }
 * }
 * }</pre>
 *
 * @param <T> the exact type of {@link Event} this subscription handles
 * @since 1.0
 * @author Imperat Framework
 * @see EventBus#register(Class, Consumer)
 * @see EventBus#unregister(UUID)
 * @see Priority
 * @see ExecutionStrategy
 */
public interface EventSubscription<T extends Event> {

    /**
     * Returns the unique identifier of this subscription.
     *
     * <p>This ID is automatically generated when the subscription is created
     * and is used to unregister the subscription via {@link EventBus#unregister(UUID)}.</p>
     *
     * @return the unique identifier, never null
     */
    UUID id();

    /**
     * Returns the handler consumer that processes events for this subscription.
     *
     * @return the event handler, never null
     */
    Consumer<T> handler();

    /**
     * Returns the priority at which this subscription executes relative to others
     * registered for the same event type.
     *
     * <p>Higher priority subscriptions execute before lower ones. For subscriptions
     * with equal priority, execution order follows registration order (FIFO).</p>
     *
     * @return the priority, never null
     * @see Priority
     */
    Priority priority();

    /**
     * Returns the execution strategy for this subscription, determining whether
     * the handler runs in the posting thread ({@link ExecutionStrategy#SYNC}) or
     * in a background thread ({@link ExecutionStrategy#ASYNC}).
     *
     * @return the execution strategy, never null
     * @see ExecutionStrategy
     */
    ExecutionStrategy strategy();
}

