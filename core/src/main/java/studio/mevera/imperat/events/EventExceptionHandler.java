package studio.mevera.imperat.events;

/**
 * A functional interface for handling exceptions that occur during event handler execution.
 * 
 * <p>When a handler throws an exception, the event bus will invoke this handler with
 * full context information, allowing for detailed logging, monitoring, or recovery actions.
 * The exception handler prevents one failing handler from breaking the entire event pipeline.</p>
 *
 * <p>The exception handler is invoked <strong>synchronously</strong> in the same thread
 * where the exception occurred, regardless of whether the failing handler was executing
 * synchronously or asynchronously.</p>
 *
 * <h2>Key Characteristics</h2>
 * <ul>
 *     <li>Invoked for every handler exception, providing full error context</li>
 *     <li>Executed synchronously in the same thread as the failing handler</li>
 *     <li>Other handlers continue to execute even after one fails</li>
 *     <li>Exception handler failures are logged but do not propagate</li>
 * </ul>
 *
 * <h2>Common Use Cases</h2>
 * <ul>
 *     <li>Logging handler failures with detailed context information</li>
 *     <li>Sending error metrics to monitoring systems</li>
 *     <li>Alerting administrators of critical handler failures</li>
 *     <li>Recording handler reliability statistics</li>
 *     <li>Implementing retry logic for transient failures</li>
 * </ul>
 *
 * <h2>Example: Basic Logging</h2>
 * <pre>{@code
 * EventExceptionHandler handler = (event, exception, handlerId) -> {
 *     logger.error("Handler {} failed while processing event {}: {}",
 *         handlerId,
 *         event.getClass().getSimpleName(),
 *         exception.getMessage(),
 *         exception);
 * };
 * 
 * EventBus eventBus = EventBus.builder()
 *     .exceptionHandler(handler)
 *     .build();
 * }</pre>
 *
 * <h2>Example: Metrics and Monitoring</h2>
 * <pre>{@code
 * EventExceptionHandler handler = (event, exception, handlerId) -> {
 *     // Record exception metrics
 *     metrics.incrementCounter("event.handler.errors",
 *         "event_type", event.getClass().getSimpleName(),
 *         "exception_type", exception.getClass().getSimpleName()
 *     );
 *
 *     // Send to monitoring system
 *     monitoring.recordException(handlerId, event, exception);
 *
 *     // Log with context
 *     logger.error("Handler failure", exception);
 * };
 * }</pre>
 *
 * <h2>Example: Critical Error Alerting</h2>
 * <pre>{@code
 * EventExceptionHandler handler = (event, exception, handlerId) -> {
 *     logger.error("Handler {} failed processing {}", handlerId, event, exception);
 *
 *     // Alert for critical events
 *     if (event instanceof CommandPreRegistrationEvent) {
 *         alertService.sendAlert(
 *             "Critical handler failure in command registration",
 *             "Handler ID: " + handlerId + "\nException: " + exception.getMessage()
 *         );
 *     }
 *
 *     // Store for later analysis
 *     errorRepository.save(new HandlerError(handlerId, event, exception));
 * };
 * }</pre>
 *
 * <h2>Example: Conditional Retry Logic</h2>
 * <pre>{@code
 * EventExceptionHandler handler = (event, exception, handlerId) -> {
 *     logger.warn("Handler {} failed, checking for retry", handlerId, exception);
 *
 *     // Retry for transient failures
 *     if (exception instanceof TransientException) {
 *         EventSubscription<?> subscription = findSubscription(handlerId);
 *         if (subscription != null && shouldRetry(handlerId)) {
 *             logger.info("Retrying handler {}", handlerId);
 *             scheduler.schedule(() -> {
 *                 try {
 *                     subscription.handler().accept(event);
 *                 } catch (Exception e) {
 *                     logger.error("Retry failed for handler {}", handlerId, e);
 *                 }
 *             }, 1, TimeUnit.SECONDS);
 *         }
 *     }
 * };
 * }</pre>
 *
 * <h2>Example: Handler Reliability Tracking</h2>
 * <pre>{@code
 * Map<UUID, HandlerStats> handlerStats = new ConcurrentHashMap<>();
 *
 * EventExceptionHandler handler = (event, exception, handlerId) -> {
 *     // Track failure statistics
 *     handlerStats.computeIfAbsent(handlerId, id -> new HandlerStats())
 *                 .recordFailure(exception);
 *
 *     HandlerStats stats = handlerStats.get(handlerId);
 *     logger.error("Handler {} failed (failures: {}, success rate: {}%)",
 *         handlerId, stats.getFailureCount(), stats.getSuccessRate());
 *
 *     // Disable problematic handlers
 *     if (stats.getSuccessRate() < 50.0) {
 *         logger.warn("Disabling unreliable handler {}", handlerId);
 *         eventBus.unregister(handlerId);
 *     }
 * };
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *     <li>Always log exceptions with sufficient context for debugging</li>
 *     <li>Keep exception handler logic fast and simple</li>
 *     <li>Avoid throwing exceptions from the exception handler itself</li>
 *     <li>Consider async logging/metrics to avoid blocking</li>
 *     <li>Use structured logging for better searchability</li>
 * </ul>
 *
 * <p><strong>Isolation Guarantee:</strong> When an exception occurs, the exception handler
 * is called, but the event pipeline continues. Other handlers will still execute even if
 * one handler fails.</p>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see EventBus
 * @see EventBus.Builder#exceptionHandler(EventExceptionHandler)
 */
public interface EventExceptionHandler {
    
    /**
     * Handles an exception that occurred during event handler execution.
     *
     * <p>This method is called synchronously whenever a handler throws an exception.
     * Implementations should be fast and should not throw exceptions themselves.</p>
     *
     * @param event        the event that was being processed when the exception occurred
     * @param exception    the exception that was thrown by the handler
     * @param subscription the unique identifier of the handler that threw the exception
     */
    <E extends Event> void handle(E event, Throwable exception, EventSubscription<E> subscription);
}

