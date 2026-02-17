package studio.mevera.imperat.events;

/**
 * An extension of {@link Event} that supports cancellation.
 *
 * <p>Events implementing this interface can be cancelled by handlers,
 * preventing subsequent actions or operations from occurring. This is
 * particularly useful for pre-processing events where validation or
 * authorization checks may need to prevent the main action.</p>
 *
 * <p><strong>Important:</strong> The event bus does NOT automatically skip
 * handlers for cancelled events. It is the responsibility of each handler
 * to check {@link #isCancelled()} if they should respect cancellation.
 * This design provides maximum flexibility, allowing some handlers to
 * process even cancelled events (e.g., for logging purposes).</p>
 *
 * <h2>Cancellation Semantics</h2>
 * <ul>
 *     <li>Cancellation is cooperative - handlers must explicitly check the flag</li>
 *     <li>Higher priority handlers can cancel events before lower priority handlers run</li>
 *     <li>Cancelled events still propagate through all registered handlers</li>
 *     <li>Events can be un-cancelled by calling {@code setCancelled(false)}</li>
 * </ul>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class PlayerChatEvent implements CancellableEvent {
 *     private final Player player;
 *     private String message;
 *     private boolean cancelled;
 *     
 *     public PlayerChatEvent(Player player, String message) {
 *         this.player = player;
 *         this.message = message;
 *         this.cancelled = false;
 *     }
 *     
 *     @Override
 *     public boolean isCancelled() {
 *         return cancelled;
 *     }
 *     
 *     @Override
 *     public void setCancelled(boolean cancelled) {
 *         this.cancelled = cancelled;
 *     }
 *     
 *     public Player getPlayer() {
 *         return player;
 *     }
 *
 *     public String getMessage() {
 *         return message;
 *     }
 *
 *     public void setMessage(String message) {
 *         this.message = message;
 *     }
 * }
 * }</pre>
 *
 * <h2>Example: Respecting Cancellation</h2>
 * <pre>{@code
 * // Handler that respects cancellation
 * eventBus.register(PlayerChatEvent.class, event -> {
 *     if (event.isCancelled()) {
 *         return; // Skip processing if already cancelled
 *     }
 *     broadcastMessage(event.getPlayer(), event.getMessage());
 * }, Priority.NORMAL);
 * }</pre>
 *
 * <h2>Example: Validation Handler</h2>
 * <pre>{@code
 * // High priority handler that validates and may cancel
 * eventBus.register(PlayerChatEvent.class, event -> {
 *     String message = event.getMessage();
 *
 *     // Check for profanity
 *     if (containsProfanity(message)) {
 *         event.setCancelled(true);
 *         event.getPlayer().sendMessage("Please avoid profanity!");
 *         return;
 *     }
 *
 *     // Check for spam
 *     if (isSpam(event.getPlayer(), message)) {
 *         event.setCancelled(true);
 *         event.getPlayer().sendMessage("Please don't spam!");
 *     }
 * }, Priority.HIGH);
 * }</pre>
 *
 * <h2>Example: Logging Cancelled Events</h2>
 * <pre>{@code
 * // Logger that runs even for cancelled events
 * eventBus.register(PlayerChatEvent.class, event -> {
 *     if (event.isCancelled()) {
 *         logger.info("Cancelled chat from {}: {}",
 *                     event.getPlayer().getName(),
 *                     event.getMessage());
 *     } else {
 *         logger.info("Chat from {}: {}",
 *                     event.getPlayer().getName(),
 *                     event.getMessage());
 *     }
 * }, Priority.LOWEST); // Run last to log final state
 * }</pre>
 *
 * <h2>Example: Un-cancelling Events</h2>
 * <pre>{@code
 * // Admin override handler that can un-cancel events
 * eventBus.register(PlayerChatEvent.class, event -> {
 *     Player player = event.getPlayer();
 *
 *     // Admins can bypass cancellation
 *     if (event.isCancelled() && player.hasPermission("chat.bypass")) {
 *         event.setCancelled(false);
 *         logger.info("Admin {} bypassed chat cancellation", player.getName());
 *     }
 * }, Priority.LOW); // Run after validation handlers
 * }</pre>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see Event
 * @see EventBus
 */
public interface CancellableEvent extends Event {
    
    /**
     * Checks if this event has been cancelled.
     *
     * @return {@code true} if the event is cancelled, {@code false} otherwise
     */
    boolean isCancelled();
    
    /**
     * Sets the cancellation state of this event.
     *
     * <p>Setting this to {@code true} typically indicates that the main action
     * associated with this event should not proceed. However, handlers must
     * explicitly check {@link #isCancelled()} and decide how to respond.</p>
     *
     * @param cancelled {@code true} to cancel the event, {@code false} to un-cancel it
     */
    void setCancelled(boolean cancelled);
}

