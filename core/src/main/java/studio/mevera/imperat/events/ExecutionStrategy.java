package studio.mevera.imperat.events;

/**
 * Defines the execution strategy for event handlers registered with the {@link EventBus}.
 *
 * <p>Each handler can be registered with a specific execution strategy,
 * determining whether it runs synchronously or asynchronously when an
 * event is posted.</p>
 *
 * <h2>Synchronous Execution ({@link #SYNC})</h2>
 * <p>Handlers execute in the same thread that posted the event.
 * The {@link EventBus#post(Event)} method blocks until all
 * synchronous handlers complete.</p>
 * <ul>
 *     <li><b>Use when:</b> Handler must complete before the event source continues</li>
 *     <li><b>Use when:</b> Handler modifies event data that other handlers need</li>
 *     <li><b>Use when:</b> Handler performs quick operations (validation, filtering)</li>
 *     <li><b>Use when:</b> Thread-local context must be preserved</li>
 * </ul>
 *
 * <h2>Asynchronous Execution ({@link #ASYNC})</h2>
 * <p>Handlers are submitted to an {@link java.util.concurrent.ExecutorService}
 * and execute in a separate thread. The {@link EventBus#post(Event)}
 * method returns immediately without waiting for async handlers to complete.</p>
 * <ul>
 *     <li><b>Use when:</b> Handler performs slow I/O operations (database, network)</li>
 *     <li><b>Use when:</b> Handler is non-critical and can run in background</li>
 *     <li><b>Use when:</b> Handler doesn't need to block the event source</li>
 *     <li><b>Use when:</b> Handler logs or records metrics asynchronously</li>
 * </ul>
 *
 * <h2>Mixed Execution Order</h2>
 * <p>When both sync and async handlers are registered for the same event type:</p>
 * <ol>
 *     <li>All {@link #SYNC} handlers execute sequentially in priority order</li>
 *     <li>All {@link #ASYNC} handlers are submitted in priority order</li>
 *     <li>The {@link EventBus#post(Event)} method returns after sync handlers complete</li>
 *     <li>Async handlers may still be running after post() returns</li>
 * </ol>
 *
 * <h2>Example: Critical Synchronous Handler</h2>
 * <pre>{@code
 * // Validation must complete before event continues
 * eventBus.register(PlayerChatEvent.class,
 *     event -> {
 *         if (containsProfanity(event.getMessage())) {
 *             event.setCancelled(true);
 *             event.getPlayer().sendMessage("Please avoid profanity!");
 *         }
 *     },
 *     Priority.HIGH,
 *     ExecutionStrategy.SYNC
 * );
 * }</pre>
 *
 * <h2>Example: Background Async Handler</h2>
 * <pre>{@code
 * // Log to database asynchronously without blocking
 * eventBus.register(PlayerChatEvent.class,
 *     event -> {
 *         if (!event.isCancelled()) {
 *             database.logChatMessage(
 *                 event.getPlayer().getId(),
 *                 event.getMessage(),
 *                 System.currentTimeMillis()
 *             );
 *         }
 *     },
 *     Priority.NORMAL,
 *     ExecutionStrategy.ASYNC
 * );
 * }</pre>
 *
 * <h2>Example: Mixed Strategies</h2>
 * <pre>{@code
 * // High priority sync validation
 * eventBus.register(CommandPreRegistrationEvent.class,
 *     event -> {
 *         if (!isCommandAllowed(event.getCommand())) {
 *             event.setCancelled(true);
 *         }
 *     },
 *     Priority.HIGH,
 *     ExecutionStrategy.SYNC
 * );
 *
 * // Low priority async logging
 * eventBus.register(CommandPreRegistrationEvent.class,
 *     event -> {
 *         logger.info("RootCommand registration attempt: {}",
 *                     event.getCommand().getName());
 *     },
 *     Priority.LOW,
 *     ExecutionStrategy.ASYNC
 * );
 * }</pre>
 *
 * <h2>Example: Event Modification</h2>
 * <pre>{@code
 * // Sync handlers can modify event data for subsequent handlers
 * eventBus.register(PlayerChatEvent.class,
 *     event -> {
 *         // Transform message before other handlers see it
 *         String filtered = filterProfanity(event.getMessage());
 *         event.setMessage(filtered);
 *     },
 *     Priority.HIGHEST,
 *     ExecutionStrategy.SYNC
 * );
 *
 * eventBus.register(PlayerChatEvent.class,
 *     event -> {
 *         // This handler sees the filtered message
 *         broadcast(event.getMessage());
 *     },
 *     Priority.NORMAL,
 *     ExecutionStrategy.SYNC
 * );
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *     <li><b>SYNC:</b> Fast execution, blocks caller, preserves order</li>
 *     <li><b>ASYNC:</b> Non-blocking, parallel execution, eventual completion</li>
 *     <li><b>Thread Safety:</b> Async handlers must be thread-safe</li>
 *     <li><b>Context:</b> Async handlers lose thread-local context</li>
 * </ul>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see EventBus#register(Class, java.util.function.Consumer, studio.mevera.imperat.util.Priority, ExecutionStrategy)
 */
public enum ExecutionStrategy {
    
    /**
     * Handler executes synchronously in the posting thread.
     *
     * <p>The {@link EventBus#post(Event)} call blocks until this handler completes.
     * Use for critical operations that must finish before the event source continues.</p>
     */
    SYNC,
    
    /**
     * Handler executes asynchronously in a separate thread.
     *
     * <p>The {@link EventBus#post(Event)} call returns immediately without waiting.
     * Use for slow operations, logging, or non-critical background tasks.</p>
     */
    ASYNC
}

