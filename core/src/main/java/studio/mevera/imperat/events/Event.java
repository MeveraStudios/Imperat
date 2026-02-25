package studio.mevera.imperat.events;

/**
 * Base marker interface for all events that can be handled by the {@link EventBus}.
 *
 * <p>Only events implementing this interface are eligible for registration
 * and processing through the event bus system. This design ensures type safety
 * and prevents arbitrary objects from being posted as events.</p>
 *
 * <p>This is a marker interface with no methods, serving purely as a contract
 * that identifies classes as events within the framework's event system.</p>
 *
 * <h2>Creating Custom Events</h2>
 * <p>To create a custom event, simply implement this interface and add any
 * relevant data fields and accessor methods:</p>
 * <pre>{@code
 * public class PlayerJoinEvent implements Event {
 *     private final Player player;
 *     private final String joinMessage;
 *
 *     public PlayerJoinEvent(Player player, String joinMessage) {
 *         this.player = player;
 *         this.joinMessage = joinMessage;
 *     }
 *
 *     public Player getPlayer() {
 *         return player;
 *     }
 *
 *     public String getJoinMessage() {
 *         return joinMessage;
 *     }
 * }
 * }</pre>
 *
 * <h2>Cancellable Events</h2>
 * <p>For events that support cancellation, implement {@link CancellableEvent} instead:</p>
 * <pre>{@code
 * public class PlayerChatEvent implements CancellableEvent {
 *     private final Player player;
 *     private String message;
 *     private boolean cancelled;
 *
 *     public PlayerChatEvent(Player player, String message) {
 *         this.player = player;
 *         this.message = message;
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
 * }
 * }</pre>
 *
 * <h2>RootCommand-Related Events</h2>
 * <p>For events related to command operations, extend {@link CommandEvent}:</p>
 * <pre>{@code
 * public class CommandExecutionEvent<S extends Source> extends CommandEvent<S> {
 *     private final ExecutionContext<S> context;
 *     private final long executionTime;
 *
 *     public CommandExecutionEvent(RootCommand<S> command,
 *                                  ExecutionContext<S> context,
 *                                  long executionTime) {
 *         super(command);
 *         this.context = context;
 *         this.executionTime = executionTime;
 *     }
 *
 *     public ExecutionContext<S> getContext() {
 *         return context;
 *     }
 *
 *     public long getExecutionTime() {
 *         return executionTime;
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Register a handler for your custom event
 * eventBus.register(PlayerJoinEvent.class, event -> {
 *     Player player = event.getPlayer();
 *     logger.info("Player joined: " + player.getName());
 *     broadcastMessage(event.getJoinMessage());
 * });
 *
 * // Post the event
 * eventBus.post(new PlayerJoinEvent(player, "Welcome!"));
 * }</pre>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see CancellableEvent
 * @see CommandEvent
 * @see EventBus
 */
public interface Event {
    // Marker interface - no methods required
}


